package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.common.DateTimeUtil;
import com.campus.volunteer.entity.StageWindow;
import com.campus.volunteer.mapper.StageWindowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阶段引擎：stage_window 8 行驱动全站开关。
 */
@Service
@RequiredArgsConstructor
public class StageService {

    private final StageWindowMapper stageWindowMapper;
    private final AuditService auditService;

    /** 全部环节（按 stage_no 升序） */
    public List<StageWindow> all() {
        return stageWindowMapper.selectList(new LambdaQueryWrapper<StageWindow>()
                .orderByAsc(StageWindow::getStageNo));
    }

    /** 按编码取环节，不存在直接报错 */
    public StageWindow window(String code) {
        StageWindow window = stageWindowMapper.selectOne(new LambdaQueryWrapper<StageWindow>()
                .eq(StageWindow::getStageCode, code));
        if (window == null) {
            throw new BizException("环节不存在：" + code);
        }
        return window;
    }

    /** 计算环节相对当前时间的状态 */
    public String phase(StageWindow window, LocalDateTime now) {
        if (now.isBefore(window.getStartTime())) {
            return Const.Phase.PENDING;
        }
        if (now.isAfter(window.getEndTime())) {
            return Const.Phase.CLOSED;
        }
        return Const.Phase.OPEN;
    }

    /** 环节是否处于开放窗口 */
    public boolean isOpen(String code) {
        StageWindow window = stageWindowMapper.selectOne(new LambdaQueryWrapper<StageWindow>()
                .eq(StageWindow::getStageCode, code));
        if (window == null) {
            return false;
        }
        return Const.Phase.OPEN.equals(phase(window, LocalDateTime.now()));
    }

    /** 环节是否已结束 */
    public boolean isEnded(String code) {
        StageWindow window = stageWindowMapper.selectOne(new LambdaQueryWrapper<StageWindow>()
                .eq(StageWindow::getStageCode, code));
        if (window == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(window.getEndTime());
    }

    /** 校验环节开放，否则抛中文业务异常 */
    public void requireOpen(String code) {
        StageWindow window = window(code);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(window.getStartTime())) {
            throw new BizException("当前不在" + window.getName() + "时间窗内（尚未开始）");
        }
        if (now.isAfter(window.getEndTime())) {
            throw new BizException("当前不在" + window.getName() + "时间窗内（已结束）");
        }
    }

    /** 当前环节实体：多个开放取 stage_no 最小；无开放取最近已开始的环节 */
    public StageWindow currentWindow() {
        LocalDateTime now = LocalDateTime.now();
        List<StageWindow> list = all();
        StageWindow open = null;
        for (StageWindow window : list) {
            if (Const.Phase.OPEN.equals(phase(window, now)) && (open == null || window.getStageNo() < open.getStageNo())) {
                open = window;
            }
        }
        if (open != null) {
            return open;
        }
        StageWindow last = null;
        for (StageWindow window : list) {
            if (!now.isBefore(window.getStartTime())) {
                last = window;
            }
        }
        if (last == null && !list.isEmpty()) {
            last = list.get(0);
        }
        return last;
    }

    /** GET /api/stage/current */
    public Map<String, Object> current() {
        StageWindow window = currentWindow();
        if (window == null) {
            throw new BizException("尚未配置环节时间窗");
        }
        return toVo(window, phase(window, LocalDateTime.now()));
    }

    /** GET /api/stage/list */
    public List<Map<String, Object>> list() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (StageWindow window : all()) {
            result.add(toVo(window, phase(window, now)));
        }
        return result;
    }

    private Map<String, Object> toVo(StageWindow window, String phase) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("code", window.getStageCode());
        vo.put("no", window.getStageNo());
        vo.put("name", window.getName());
        vo.put("startTime", window.getStartTime());
        vo.put("endTime", window.getEndTime());
        vo.put("phase", phase);
        vo.put("remark", window.getRemark());
        return vo;
    }

    /** 学院管理员改期 */
    @Transactional
    public void updateWindow(String code, String startTime, String endTime) {
        com.campus.volunteer.common.CurrentUser.requireRole(Const.Role.COLLEGE_ADMIN);
        StageWindow window = window(code);
        LocalDateTime start = DateTimeUtil.parse(startTime);
        LocalDateTime end = DateTimeUtil.parse(endTime);
        if (!end.isAfter(start)) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        window.setStartTime(start);
        window.setEndTime(end);
        stageWindowMapper.updateById(window);
        auditService.log("STAGE_UPDATE", code,
                "时间窗调整为 " + DateTimeUtil.format(start) + " ~ " + DateTimeUtil.format(end));
    }

    /** 演示用：一键推进到下一环节 */
    @Transactional
    public Map<String, Object> advance() {
        com.campus.volunteer.common.CurrentUser.requireRole(Const.Role.COLLEGE_ADMIN);
        LocalDateTime now = LocalDateTime.now();
        StageWindow current = currentWindow();
        if (current == null) {
            throw new BizException("尚未配置环节时间窗");
        }
        Duration duration = Duration.between(current.getStartTime(), current.getEndTime());
        if (duration.isNegative() || duration.isZero()) {
            duration = Duration.ofHours(1);
        }
        // 前后两个环节各退 1 秒，避免同一秒内被判定为「上一环节仍开着」
        LocalDateTime boundary = now.minusSeconds(1);
        current.setEndTime(boundary);
        stageWindowMapper.updateById(current);

        StageWindow next = stageWindowMapper.selectOne(new LambdaQueryWrapper<StageWindow>()
                .eq(StageWindow::getStageNo, current.getStageNo() + 1));
        if (next != null) {
            next.setStartTime(boundary);
            next.setEndTime(now.plus(duration));
            stageWindowMapper.updateById(next);
        }
        auditService.log("STAGE_ADVANCE", current.getStageCode(),
                "结束环节 " + current.getStageCode() + (next == null ? "（已到最后一个环节）" : "，开启 " + next.getStageCode()));
        return current();
    }
}
