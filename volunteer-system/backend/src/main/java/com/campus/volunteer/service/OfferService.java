package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.dto.OfferBatchReq;
import com.campus.volunteer.entity.Intent;
import com.campus.volunteer.entity.Lab;
import com.campus.volunteer.entity.Mentor;
import com.campus.volunteer.entity.Offer;
import com.campus.volunteer.entity.StageWindow;
import com.campus.volunteer.entity.SysUser;
import com.campus.volunteer.mapper.IntentMapper;
import com.campus.volunteer.mapper.LabMapper;
import com.campus.volunteer.mapper.MentorMapper;
import com.campus.volunteer.mapper.OfferMapper;
import com.campus.volunteer.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 拟录取服务：导师批量提交、学生确认、递补顺位补位。
 */
@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferMapper offerMapper;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final SysUserMapper sysUserMapper;
    private final IntentMapper intentMapper;
    private final StageService stageService;
    private final QuotaService quotaService;
    private final MentorService mentorService;
    private final AuditService auditService;

    /** 导师批量提交拟录取 / 递补 / 内部调剂候选 */
    @Transactional
    public Map<String, Object> batch(OfferBatchReq req) {
        CurrentUser.requireRole(Const.Role.MENTOR);
        Mentor mentor = mentorService.current();
        int round = req.getRound() == null ? quotaService.currentRound() : req.getRound();
        List<Long> offerStudents = req.getOffers() == null ? new ArrayList<>() : req.getOffers();
        List<Long> waitStudents = req.getWaitlist() == null ? new ArrayList<>() : req.getWaitlist();
        List<Long> innerStudents = req.getInner() == null ? new ArrayList<>() : req.getInner();

        // ========== 三名单交叉去重：一个学生只能进一份名单（offer > waitlist > inner） ==========
        Set<Long> seen = new HashSet<>();
        List<Long> offerClean = new ArrayList<>();
        for (Long sid : offerStudents) {
            if (sid != null && seen.add(sid)) offerClean.add(sid);
        }
        List<Long> waitClean = new ArrayList<>();
        for (Long sid : waitStudents) {
            if (sid != null && seen.add(sid)) waitClean.add(sid);
        }
        List<Long> innerClean = new ArrayList<>();
        for (Long sid : innerStudents) {
            if (sid != null && seen.add(sid)) innerClean.add(sid);
        }
        offerStudents = offerClean;
        waitStudents = waitClean;
        innerStudents = innerClean;

        // 把本导师本轮已有的其他类型 offer（如学生现在被改列到 WAITLIST，则旧的 OFFER 置 FILLED）
        List<Offer> existedAll = offerMapper.selectList(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getMentorId, mentor.getId())
                .eq(Offer::getRoundNo, round)
                .in(Offer::getStatus, Const.OfferStatus.PENDING));
        Map<Long, String> incomingType = new HashMap<>();
        for (Long sid : offerStudents) incomingType.put(sid, Const.OfferType.OFFER);
        for (Long sid : waitStudents) incomingType.put(sid, Const.OfferType.WAITLIST);
        for (Long sid : innerStudents) incomingType.put(sid, Const.OfferType.INNER);
        for (Offer old : existedAll) {
            String targetType = incomingType.get(old.getStudentId());
            if (targetType != null && !targetType.equals(old.getType())) {
                // 该学生本次被放到了另一份名单，旧的置为 FILLED
                old.setStatus(Const.OfferStatus.FILLED);
                offerMapper.updateById(old);
            }
        }

        // 名额校验：已占用 OFFER 数 + 本次新增（排除重复提交）≤ mentor.quota
        long used = quotaService.usedByMentor(mentor.getId(), round);
        int newCount = offerStudents.size();
        if (!offerStudents.isEmpty()) {
            List<Offer> existed = offerMapper.selectList(new LambdaQueryWrapper<Offer>()
                    .eq(Offer::getMentorId, mentor.getId())
                    .eq(Offer::getRoundNo, round)
                    .eq(Offer::getType, Const.OfferType.OFFER)
                    .in(Offer::getStatus, Const.OfferStatus.PENDING, Const.OfferStatus.CONFIRMED)
                    .in(Offer::getStudentId, offerStudents));
            newCount -= existed.size();
        }
        if (used + newCount > mentor.getQuota()) {
            throw new BizException("超出导师名额：计划 " + mentor.getQuota() + " 人，已占用 " + used + " 人，本次新增 " + newCount + " 人");
        }

        int offerCount = 0;
        for (Long studentId : offerStudents) {
            if (studentId == null) {
                continue;
            }
            // 已在其他导师处确认的学生直接跳过
            if (hasConfirmedOffer(studentId)) {
                continue;
            }
            upsertOffer(studentId, mentor.getId(), mentor.getLabId(), round, Const.OfferType.OFFER,
                    Const.OfferStatus.PENDING, null);
            updateIntentStatus(studentId, mentor.getId(), round, Const.IntentStatus.OFFERED);
            offerCount++;
        }
        for (int i = 0; i < waitStudents.size(); i++) {
            Long studentId = waitStudents.get(i);
            if (studentId == null) {
                continue;
            }
            upsertOffer(studentId, mentor.getId(), mentor.getLabId(), round, Const.OfferType.WAITLIST,
                    Const.OfferStatus.PENDING, i + 1);
        }
        for (Long studentId : innerStudents) {
            if (studentId == null) {
                continue;
            }
            upsertOffer(studentId, mentor.getId(), mentor.getLabId(), round, Const.OfferType.INNER,
                    Const.OfferStatus.PENDING, null);
        }
        auditService.log("OFFER_BATCH", "mentor:" + mentor.getId(),
                "第" + round + "轮提交：拟录取 " + offerCount + "，递补 " + waitStudents.size() + "，内部调剂 " + innerStudents.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("round", round);
        result.put("offerCount", offerCount);
        result.put("waitlistCount", waitStudents.size());
        result.put("innerCount", innerStudents.size());
        return result;
    }

    /** 学生查看自己的拟录取结果 */
    public Map<String, Object> my() {
        CurrentUser.requireRole(Const.Role.STUDENT);
        Long studentId = CurrentUser.id();
        List<Offer> offers = offerMapper.selectList(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getStudentId, studentId)
                .orderByAsc(Offer::getRoundNo)
                .orderByAsc(Offer::getId));
        Map<Long, Mentor> mentors = new HashMap<>();
        Map<Long, String> labNames = new HashMap<>();
        if (!offers.isEmpty()) {
            Set<Long> mentorIds = new HashSet<>();
            Set<Long> labIds = new HashSet<>();
            for (Offer offer : offers) {
                mentorIds.add(offer.getMentorId());
                labIds.add(offer.getLabId());
            }
            for (Mentor mentor : mentorMapper.selectBatchIds(mentorIds)) {
                mentors.put(mentor.getId(), mentor);
            }
            for (Lab lab : labMapper.selectBatchIds(labIds)) {
                labNames.put(lab.getId(), lab.getName());
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        boolean canConfirm = false;
        for (Offer offer : offers) {
            Mentor mentor = mentors.get(offer.getMentorId());
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", offer.getId());
            vo.put("mentorId", offer.getMentorId());
            vo.put("mentorName", mentor == null ? "" : mentor.getName());
            vo.put("labId", offer.getLabId());
            vo.put("labName", labNames.get(offer.getLabId()));
            vo.put("round", offer.getRoundNo());
            vo.put("type", offer.getType());
            vo.put("status", offer.getStatus());
            vo.put("rankNo", offer.getRankNo());
            rows.add(vo);
            if (Const.OfferType.OFFER.equals(offer.getType()) && Const.OfferStatus.PENDING.equals(offer.getStatus())) {
                canConfirm = true;
            }
        }
        LocalDateTime deadline = null;
        StageWindow confirmStage;
        try {
            confirmStage = stageService.window(Const.Stage.CONFIRM);
            deadline = confirmStage.getEndTime();
        } catch (BizException ignored) {
            // 未配置确认环节时无截止时间
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("offers", rows);
        result.put("canConfirm", canConfirm && stageService.isOpen(Const.Stage.CONFIRM));
        result.put("confirmDeadline", deadline);
        return result;
    }

    /** 学生确认拟录取（多 offer 自选其一） */
    @Transactional
    public Map<String, Object> confirm(Long offerId) {
        CurrentUser.requireRole(Const.Role.STUDENT);
        Long studentId = CurrentUser.id();
        Offer offer = offerMapper.selectById(offerId);
        if (offer == null || !studentId.equals(offer.getStudentId())) {
            throw new BizException("拟录取记录不存在");
        }
        if (!Const.OfferType.OFFER.equals(offer.getType())) {
            throw new BizException("只能确认拟录取类型的记录");
        }
        if (!Const.OfferStatus.PENDING.equals(offer.getStatus())) {
            throw new BizException("该记录当前不可确认，状态：" + offer.getStatus());
        }
        confirmCore(offer);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("offerId", offer.getId());
        result.put("status", offer.getStatus());
        return result;
    }

    /** 定时任务用：自动确认唯一待确认 offer */
    @Transactional
    public void autoConfirm(Long offerId) {
        Offer offer = offerMapper.selectById(offerId);
        if (offer == null || !Const.OfferType.OFFER.equals(offer.getType())
                || !Const.OfferStatus.PENDING.equals(offer.getStatus())) {
            return;
        }
        confirmCore(offer);
    }

    /** 确认核心逻辑：置 CONFIRMED、其余 PENDING 置 DECLINED 并触发递补 */
    private void confirmCore(Offer offer) {
        if (hasConfirmedOffer(offer.getStudentId())) {
            throw new BizException("该学生已完成录取确认，不能重复确认");
        }
        offer.setStatus(Const.OfferStatus.CONFIRMED);
        offer.setConfirmTime(LocalDateTime.now());
        offerMapper.updateById(offer);
        updateIntentStatus(offer.getStudentId(), offer.getMentorId(), offer.getRoundNo(), Const.IntentStatus.CONFIRMED);
        releaseOtherIntents(offer.getStudentId(), offer.getRoundNo(), offer.getMentorId());

        List<Offer> others = offerMapper.selectList(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getStudentId, offer.getStudentId())
                .eq(Offer::getType, Const.OfferType.OFFER)
                .eq(Offer::getStatus, Const.OfferStatus.PENDING)
                .ne(Offer::getId, offer.getId()));
        boolean direct = !stageService.isOpen(Const.Stage.CONFIRM);
        for (Offer other : others) {
            other.setStatus(Const.OfferStatus.DECLINED);
            offerMapper.updateById(other);
            promoteWaitlist(other.getMentorId(), other.getRoundNo(), direct);
        }
        // 学生本人的递补/内部调剂候选不再需要
        fillWaitlistAndInner(offer.getStudentId());
        auditService.log("OFFER_CONFIRM", "offer:" + offer.getId(),
                "学生 " + offer.getStudentId() + " 确认导师 " + offer.getMentorId());
    }

    /** 导师查看本人本轮的拟录取名单 */
    public List<Map<String, Object>> mentorList(Integer round) {
        CurrentUser.requireRole(Const.Role.MENTOR);
        Mentor mentor = mentorService.current();
        int r = round == null ? quotaService.currentRound() : round;
        List<Offer> offers = offerMapper.selectList(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getMentorId, mentor.getId())
                .eq(Offer::getRoundNo, r)
                .orderByAsc(Offer::getType)
                .orderByAsc(Offer::getRankNo)
                .orderByAsc(Offer::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        if (offers.isEmpty()) {
            return rows;
        }
        Set<Long> studentIds = new HashSet<>();
        for (Offer offer : offers) {
            studentIds.add(offer.getStudentId());
        }
        Map<Long, SysUser> students = new HashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(studentIds)) {
            students.put(user.getId(), user);
        }
        for (Offer offer : offers) {
            SysUser student = students.get(offer.getStudentId());
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", offer.getId());
            vo.put("studentId", offer.getStudentId());
            vo.put("studentName", student == null ? "" : student.getRealName());
            vo.put("studentNo", student == null ? "" : student.getStudentNo());
            vo.put("type", offer.getType());
            vo.put("status", offer.getStatus());
            vo.put("rankNo", offer.getRankNo());
            vo.put("round", offer.getRoundNo());
            rows.add(vo);
        }
        return rows;
    }

    /** 是否已有确认录取 */
    public boolean hasConfirmedOffer(Long studentId) {
        return offerMapper.selectCount(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getStudentId, studentId)
                .eq(Offer::getType, Const.OfferType.OFFER)
                .eq(Offer::getStatus, Const.OfferStatus.CONFIRMED)) > 0;
    }

    /** 是否有待确认的拟录取（用于递补冲突判断） */
    public boolean hasPendingOffer(Long studentId) {
        return offerMapper.selectCount(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getStudentId, studentId)
                .eq(Offer::getType, Const.OfferType.OFFER)
                .eq(Offer::getStatus, Const.OfferStatus.PENDING)) > 0;
    }

    /**
     * 按递补顺位补位：把 WAITLIST 中无冲突的记录转成 PENDING OFFER；
     * directConfirm=true（确认窗口已关闭）时直接置 CONFIRMED。
     */
    @Transactional
    public int promoteWaitlist(Long mentorId, int round, boolean directConfirm) {
        Mentor mentor = mentorMapper.selectById(mentorId);
        if (mentor == null) {
            return 0;
        }
        int promoted = 0;
        while (true) {
            if (quotaService.remainOfMentor(mentorId, round) <= 0) {
                break;
            }
            List<Offer> waitlist = offerMapper.selectList(new LambdaQueryWrapper<Offer>()
                    .eq(Offer::getMentorId, mentorId)
                    .eq(Offer::getRoundNo, round)
                    .eq(Offer::getType, Const.OfferType.WAITLIST)
                    .eq(Offer::getStatus, Const.OfferStatus.PENDING)
                    .isNotNull(Offer::getRankNo)
                    .orderByAsc(Offer::getRankNo)
                    .orderByAsc(Offer::getId));
            Offer candidate = null;
            for (Offer item : waitlist) {
                if (hasConfirmedOffer(item.getStudentId()) || hasPendingOffer(item.getStudentId())) {
                    continue;
                }
                candidate = item;
                break;
            }
            if (candidate == null) {
                break;
            }
            Offer existed = offerMapper.selectOne(new LambdaQueryWrapper<Offer>()
                    .eq(Offer::getStudentId, candidate.getStudentId())
                    .eq(Offer::getMentorId, mentorId)
                    .eq(Offer::getRoundNo, round)
                    .eq(Offer::getType, Const.OfferType.OFFER));
            if (existed != null) {
                offerMapper.update(null, new LambdaUpdateWrapper<Offer>()
                        .eq(Offer::getId, candidate.getId())
                        .set(Offer::getStatus, Const.OfferStatus.FILLED));
            } else {
                offerMapper.update(null, new LambdaUpdateWrapper<Offer>()
                        .eq(Offer::getId, candidate.getId())
                        .set(Offer::getType, Const.OfferType.OFFER)
                        .set(Offer::getStatus, directConfirm ? Const.OfferStatus.CONFIRMED : Const.OfferStatus.PENDING)
                        .set(Offer::getRankNo, null)
                        .set(Offer::getConfirmTime, directConfirm ? LocalDateTime.now() : null));
            }
            updateIntentStatus(candidate.getStudentId(), mentorId, round,
                    directConfirm ? Const.IntentStatus.CONFIRMED : Const.IntentStatus.OFFERED);
            if (directConfirm) {
                declineOtherPendingOffers(candidate.getStudentId(), candidate.getId());
                fillWaitlistAndInner(candidate.getStudentId());
                releaseOtherIntents(candidate.getStudentId(), round, mentorId);
            }
            auditService.log("WAITLIST_PROMOTE", "mentor:" + mentorId,
                    "第" + round + "轮递补学生 " + candidate.getStudentId() + "，结果 "
                            + (directConfirm ? Const.OfferStatus.CONFIRMED : Const.OfferStatus.PENDING));
            promoted++;
        }
        return promoted;
    }

    /** 插入或更新一条 offer（唯一键 student+mentor+round+type） */
    public void upsertOffer(Long studentId, Long mentorId, Long labId, int round,
                            String type, String status, Integer rankNo) {
        Offer offer = offerMapper.selectOne(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getStudentId, studentId)
                .eq(Offer::getMentorId, mentorId)
                .eq(Offer::getRoundNo, round)
                .eq(Offer::getType, type));
        if (offer == null) {
            offer = new Offer();
            offer.setStudentId(studentId);
            offer.setMentorId(mentorId);
            offer.setLabId(labId);
            offer.setRoundNo(round);
            offer.setType(type);
            offer.setStatus(status);
            offer.setRankNo(rankNo);
            offer.setCreateTime(LocalDateTime.now());
            if (Const.OfferStatus.CONFIRMED.equals(status)) {
                offer.setConfirmTime(LocalDateTime.now());
            }
            offerMapper.insert(offer);
        } else {
            offer.setLabId(labId);
            offer.setStatus(status);
            offer.setRankNo(rankNo);
            if (Const.OfferStatus.CONFIRMED.equals(status)) {
                offer.setConfirmTime(LocalDateTime.now());
            }
            offerMapper.updateById(offer);
        }
    }

    /** 更新意向状态（存在才更新） */
    public void updateIntentStatus(Long studentId, Long mentorId, int round, String status) {
        Intent intent = intentMapper.selectOne(new LambdaQueryWrapper<Intent>()
                .eq(Intent::getStudentId, studentId)
                .eq(Intent::getMentorId, mentorId)
                .eq(Intent::getRoundNo, round));
        if (intent != null) {
            intent.setStatus(status);
            intent.setUpdateTime(LocalDateTime.now());
            intentMapper.updateById(intent);
        }
    }

    /** 同轮其他导师的意向置为已释放 */
    private void releaseOtherIntents(Long studentId, int round, Long keepMentorId) {
        List<Intent> others = intentMapper.selectList(new LambdaQueryWrapper<Intent>()
                .eq(Intent::getStudentId, studentId)
                .eq(Intent::getRoundNo, round)
                .ne(Intent::getMentorId, keepMentorId)
                .ne(Intent::getStatus, Const.IntentStatus.RELEASED));
        for (Intent intent : others) {
            intent.setStatus(Const.IntentStatus.RELEASED);
            intent.setUpdateTime(LocalDateTime.now());
            intentMapper.updateById(intent);
        }
    }

    /** 学生其他待确认 OFFER 置为拒绝（不触发递补），供调剂指派复用 */
    public void declineOtherPendingOffers(Long studentId, Long excludeOfferId) {
        LambdaQueryWrapper<Offer> wrapper = new LambdaQueryWrapper<Offer>()
                .eq(Offer::getStudentId, studentId)
                .eq(Offer::getType, Const.OfferType.OFFER)
                .eq(Offer::getStatus, Const.OfferStatus.PENDING);
        if (excludeOfferId != null) {
            wrapper.ne(Offer::getId, excludeOfferId);
        }
        List<Offer> others = offerMapper.selectList(wrapper);
        for (Offer other : others) {
            other.setStatus(Const.OfferStatus.DECLINED);
            offerMapper.updateById(other);
        }
    }

    /** 学生本人的递补/内部调剂候选不再保留 */
    private void fillWaitlistAndInner(Long studentId) {
        List<Offer> list = offerMapper.selectList(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getStudentId, studentId)
                .in(Offer::getType, Const.OfferType.WAITLIST, Const.OfferType.INNER)
                .eq(Offer::getStatus, Const.OfferStatus.PENDING));
        for (Offer offer : list) {
            offer.setStatus(Const.OfferStatus.FILLED);
            offerMapper.updateById(offer);
        }
    }
}
