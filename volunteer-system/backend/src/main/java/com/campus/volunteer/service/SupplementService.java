package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.entity.StageWindow;
import com.campus.volunteer.entity.SupplementRound;
import com.campus.volunteer.mapper.SupplementRoundMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分轮补录服务（第 2、3 轮）。
 */
@Service
@RequiredArgsConstructor
public class SupplementService {

    private final SupplementRoundMapper supplementRoundMapper;
    private final QuotaService quotaService;
    private final StageService stageService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    /** 补录轮次与实时名额 */
    public List<Map<String, Object>> rounds() {
        List<SupplementRound> rounds = supplementRoundMapper.selectList(new LambdaQueryWrapper<SupplementRound>()
                .orderByAsc(SupplementRound::getRoundNo));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SupplementRound round : rounds) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("roundNo", round.getRoundNo());
            vo.put("openFlag", round.getOpenFlag());
            vo.put("startTime", round.getStartTime());
            vo.put("endTime", round.getEndTime());
            vo.put("slots", quotaService.slots(round.getRoundNo()));
            result.add(vo);
        }
        return result;
    }

    /** 学院管理员开启某一轮补录：open_flag=1 并快照剩余名额，同时关闭上一轮 */
    @Transactional
    public Map<String, Object> open(Integer roundNo) {
        CurrentUser.requireRole(Const.Role.COLLEGE_ADMIN);
        if (roundNo == null) {
            throw new BizException("请指定补录轮次");
        }
        SupplementRound round = supplementRoundMapper.selectOne(new LambdaQueryWrapper<SupplementRound>()
                .eq(SupplementRound::getRoundNo, roundNo));
        if (round == null) {
            throw new BizException("补录轮次不存在：" + roundNo);
        }
        if (round.getOpenFlag() != null && round.getOpenFlag() == 1) {
            throw new BizException("第 " + roundNo + " 轮补录已开启，请勿重复开启");
        }
        // 关闭上一轮
        SupplementRound previous = supplementRoundMapper.selectOne(new LambdaQueryWrapper<SupplementRound>()
                .eq(SupplementRound::getRoundNo, roundNo - 1));
        if (previous != null && previous.getOpenFlag() != null && previous.getOpenFlag() == 1) {
            previous.setOpenFlag(0);
            supplementRoundMapper.updateById(previous);
        }

        List<Map<String, Object>> slots = quotaService.slots(roundNo);
        try {
            round.setSlotsJson(objectMapper.writeValueAsString(slots));
        } catch (Exception e) {
            throw new BizException("名额快照生成失败：" + e.getMessage());
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end;
        try {
            StageWindow window = stageService.window(Const.Stage.SUPPLEMENT);
            end = window.getEndTime();
            if (end == null || !end.isAfter(now)) {
                end = now.plusDays(1);
            }
        } catch (BizException e) {
            end = now.plusDays(1);
        }
        round.setOpenFlag(1);
        round.setStartTime(now);
        round.setEndTime(end);
        supplementRoundMapper.updateById(round);
        auditService.log("SUPPLEMENT_OPEN", "round:" + roundNo,
                "开启第 " + roundNo + " 轮补录，名额快照 " + slots.size() + " 个导师");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roundNo", roundNo);
        result.put("openFlag", 1);
        result.put("startTime", now);
        result.put("endTime", end);
        result.put("slots", slots);
        return result;
    }
}
