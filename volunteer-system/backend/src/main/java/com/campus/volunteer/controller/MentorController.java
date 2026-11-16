package com.campus.volunteer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.R;
import com.campus.volunteer.entity.Lab;
import com.campus.volunteer.entity.Mentor;
import com.campus.volunteer.mapper.LabMapper;
import com.campus.volunteer.mapper.MentorMapper;
import com.campus.volunteer.service.MentorService;
import com.campus.volunteer.service.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导师本人相关接口。
 */
@RestController
@RequestMapping("/api/mentor")
@RequiredArgsConstructor
public class MentorController {

    private final QuotaService quotaService;
    private final MentorService mentorService;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;

    /** 当前登录导师的名额、已用、剩余及各类候选名单数量 */
    @GetMapping("/me/quota")
    public R<Map<String, Object>> myQuota(@RequestParam(required = false) Integer round) {
        return R.ok(quotaService.myMentorQuota(round));
    }

    /** 当前登录导师的个人信息 */
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        Mentor mentor = mentorService.current();
        Map<String, Object> vo = mentorVo(mentor);
        return R.ok(vo);
    }

    /** 更新当前登录导师的个人信息：姓名、职位、所在实验室、名额、研究方向、个人简介 */
    @PutMapping("/me")
    public R<Map<String, Object>> updateMe(@RequestBody Map<String, Object> payload) {
        Mentor mentor = mentorService.current();

        Object name = payload.get("name");
        if (name != null && !String.valueOf(name).trim().isEmpty()) {
            mentor.setName(String.valueOf(name).trim());
        }
        Object title = payload.get("title");
        if (title != null) {
            mentor.setTitle(String.valueOf(title).trim());
        }
        Object direction = payload.get("direction");
        if (direction != null) {
            mentor.setDirection(String.valueOf(direction).trim());
        }
        Object intro = payload.get("intro");
        if (intro != null) {
            mentor.setIntro(String.valueOf(intro));
        }
        Object acceptCrossTransfer = payload.get("acceptCrossTransfer");
        if (acceptCrossTransfer != null) {
            mentor.setAcceptCrossTransfer(
                    Boolean.TRUE.equals(acceptCrossTransfer) ? 1 : 0);
        }

        // 实验室
        Object labIdObj = payload.get("labId");
        if (labIdObj != null) {
            Long labId;
            try {
                labId = Long.parseLong(String.valueOf(labIdObj));
            } catch (NumberFormatException e) {
                throw new BizException("实验室参数无效");
            }
            Lab lab = labMapper.selectById(labId);
            if (lab == null) {
                throw new BizException("所选实验室不存在");
            }
            mentor.setLabId(labId);
        }

        // 名额
        Object quotaObj = payload.get("quota");
        if (quotaObj != null) {
            int quota;
            try {
                quota = Integer.parseInt(String.valueOf(quotaObj));
            } catch (NumberFormatException e) {
                throw new BizException("名额必须是整数");
            }
            if (quota < 0) {
                throw new BizException("名额不能为负数");
            }
            mentor.setQuota(quota);
        }

        mentorMapper.updateById(mentor);
        return R.ok(mentorVo(mentor));
    }

    /** 所有实训室列表（导师选择自己所在实验室时用） */
    @GetMapping("/labs")
    public R<List<Map<String, Object>>> labs() {
        List<Lab> labs = labMapper.selectList(new LambdaQueryWrapper<Lab>().orderByAsc(Lab::getId));
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Lab lab : labs) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", lab.getId());
            vo.put("name", lab.getName());
            vo.put("code", lab.getCode());
            vo.put("direction", lab.getDirection());
            result.add(vo);
        }
        return R.ok(result);
    }

    private Map<String, Object> mentorVo(Mentor mentor) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", mentor.getId());
        vo.put("name", mentor.getName());
        vo.put("title", mentor.getTitle());
        vo.put("labId", mentor.getLabId());
        vo.put("direction", mentor.getDirection());
        vo.put("intro", mentor.getIntro());
        vo.put("quota", mentor.getQuota());
        vo.put("acceptCrossTransfer",
                mentor.getAcceptCrossTransfer() != null && mentor.getAcceptCrossTransfer() == 1);

        // 附加实训室名称
        Lab lab = labMapper.selectById(mentor.getLabId());
        vo.put("labName", lab == null ? "未知实验室" : lab.getName());
        return vo;
    }
}
