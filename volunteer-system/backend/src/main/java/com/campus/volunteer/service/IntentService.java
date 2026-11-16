package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.entity.Intent;
import com.campus.volunteer.entity.Lab;
import com.campus.volunteer.entity.Mentor;
import com.campus.volunteer.entity.Offer;
import com.campus.volunteer.entity.SupplementRound;
import com.campus.volunteer.mapper.IntentMapper;
import com.campus.volunteer.mapper.LabMapper;
import com.campus.volunteer.mapper.MentorMapper;
import com.campus.volunteer.mapper.OfferMapper;
import com.campus.volunteer.mapper.SupplementRoundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 意向填报服务：最多 3 个平行导师意向，覆盖式提交。
 */
@Service
@RequiredArgsConstructor
public class IntentService {

    private final IntentMapper intentMapper;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final OfferMapper offerMapper;
    private final SupplementRoundMapper supplementRoundMapper;
    private final StageService stageService;
    private final QuotaService quotaService;
    private final AuditService auditService;

    /** 我的意向 */
    public Map<String, Object> my(Integer round) {
        CurrentUser.requireRole(Const.Role.STUDENT);
        Long studentId = CurrentUser.id();
        int r = round == null ? quotaService.currentRound() : round;
        List<Intent> intents = intentMapper.selectList(new LambdaQueryWrapper<Intent>()
                .eq(Intent::getStudentId, studentId)
                .eq(Intent::getRoundNo, r)
                .orderByAsc(Intent::getId));

        boolean acceptInner = false;
        boolean acceptCross = false;
        List<Map<String, Object>> items = new ArrayList<>();
        if (!intents.isEmpty()) {
            acceptInner = intents.get(0).getAcceptInner() != null && intents.get(0).getAcceptInner() == 1;
            acceptCross = intents.get(0).getAcceptCross() != null && intents.get(0).getAcceptCross() == 1;
        }
        Map<Long, Mentor> mentors = new HashMap<>();
        if (!intents.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (Intent intent : intents) {
                ids.add(intent.getMentorId());
            }
            for (Mentor mentor : mentorMapper.selectBatchIds(ids)) {
                mentors.put(mentor.getId(), mentor);
            }
        }
        Map<Long, String> labNames = new HashMap<>();
        for (Lab lab : labMapper.selectList(new LambdaQueryWrapper<Lab>())) {
            labNames.put(lab.getId(), lab.getName());
        }
        for (Intent intent : intents) {
            Mentor mentor = mentors.get(intent.getMentorId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", intent.getId());
            item.put("mentorId", intent.getMentorId());
            item.put("mentorName", mentor == null ? "未知导师" : mentor.getName());
            item.put("labId", intent.getLabId());
            item.put("labName", labNames.get(intent.getLabId()));
            item.put("status", intent.getStatus());
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("round", r);
        result.put("acceptInner", acceptInner);
        result.put("acceptCross", acceptCross);
        result.put("items", items);
        return result;
    }

    /** 提交意向（覆盖式） */
    @Transactional
    public Map<String, Object> submit(com.campus.volunteer.dto.IntentSubmitReq req) {
        CurrentUser.requireRole(Const.Role.STUDENT);
        Long studentId = CurrentUser.id();
        int round = req.getRound() == null ? 1 : req.getRound();

        // 环节校验
        if (round == 1) {
            stageService.requireOpen(Const.Stage.INTENT);
        } else {
            stageService.requireOpen(Const.Stage.SUPPLEMENT);
            SupplementRound sr = supplementRoundMapper.selectOne(new LambdaQueryWrapper<SupplementRound>()
                    .eq(SupplementRound::getRoundNo, round));
            if (sr == null || sr.getOpenFlag() == null || sr.getOpenFlag() != 1) {
                throw new BizException("第" + round + "轮补录尚未开启");
            }
        }

        // 已确认录取的学生禁止再填
        long confirmed = offerMapper.selectCount(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getStudentId, studentId)
                .eq(Offer::getType, Const.OfferType.OFFER)
                .eq(Offer::getStatus, Const.OfferStatus.CONFIRMED));
        if (confirmed > 0) {
            throw new BizException("你已完成录取确认，不能再填报意向");
        }

        List<Long> mentorIds = req.getMentorIds();
        if (CollectionUtils.isEmpty(mentorIds)) {
            throw new BizException("请至少选择一个导师意向");
        }
        if (mentorIds.size() > 3) {
            throw new BizException("最多填报 3 个平行导师意向");
        }
        Set<Long> distinct = new HashSet<>(mentorIds);
        if (distinct.size() != mentorIds.size()) {
            throw new BizException("同一导师不可重复填报");
        }
        List<Mentor> mentors = mentorMapper.selectBatchIds(mentorIds);
        if (mentors.size() != mentorIds.size()) {
            throw new BizException("存在无效的导师意向");
        }

        // 保留 OFFERED/CONFIRMED 的历史记录，其余覆盖删除
        List<Intent> existing = intentMapper.selectList(new LambdaQueryWrapper<Intent>()
                .eq(Intent::getStudentId, studentId)
                .eq(Intent::getRoundNo, round));
        Set<Long> lockedMentorIds = new HashSet<>();
        for (Intent old : existing) {
            if (Const.IntentStatus.OFFERED.equals(old.getStatus()) || Const.IntentStatus.CONFIRMED.equals(old.getStatus())) {
                lockedMentorIds.add(old.getMentorId());
            }
        }
        intentMapper.delete(new LambdaQueryWrapper<Intent>()
                .eq(Intent::getStudentId, studentId)
                .eq(Intent::getRoundNo, round)
                .notIn(Intent::getStatus, Const.IntentStatus.OFFERED, Const.IntentStatus.CONFIRMED));

        int acceptInner = Boolean.TRUE.equals(req.getAcceptInner()) ? 1 : 0;
        int acceptCross = Boolean.TRUE.equals(req.getAcceptCross()) ? 1 : 0;
        int inserted = 0;
        for (Mentor mentor : mentors) {
            if (lockedMentorIds.contains(mentor.getId())) {
                continue;
            }
            Intent intent = new Intent();
            intent.setStudentId(studentId);
            intent.setMentorId(mentor.getId());
            intent.setLabId(mentor.getLabId());
            intent.setRoundNo(round);
            intent.setAcceptInner(acceptInner);
            intent.setAcceptCross(acceptCross);
            intent.setStatus(Const.IntentStatus.SUBMITTED);
            intent.setCreateTime(LocalDateTime.now());
            intent.setUpdateTime(LocalDateTime.now());
            intentMapper.insert(intent);
            inserted++;
        }
        auditService.log("INTENT_SUBMIT", "student:" + studentId,
                "第" + round + "轮填报 " + inserted + " 个意向，内部调剂=" + acceptInner + "，跨室调剂=" + acceptCross);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("round", round);
        result.put("count", inserted);
        return result;
    }
}
