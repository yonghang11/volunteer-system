package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.common.DateTimeUtil;
import com.campus.volunteer.entity.AuditLog;
import com.campus.volunteer.entity.JoinRecord;
import com.campus.volunteer.entity.Offer;
import com.campus.volunteer.entity.PublicList;
import com.campus.volunteer.entity.StageWindow;
import com.campus.volunteer.entity.SysUser;
import com.campus.volunteer.mapper.AuditLogMapper;
import com.campus.volunteer.mapper.JoinRecordMapper;
import com.campus.volunteer.mapper.OfferMapper;
import com.campus.volunteer.mapper.PublicListMapper;
import com.campus.volunteer.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务：自动确认、逾期释放与递补、公示期满入组、环节切换通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoTaskService {

    private final StageService stageService;
    private final OfferService offerService;
    private final OfferMapper offerMapper;
    private final PublicListMapper publicListMapper;
    private final JoinRecordMapper joinRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final AuditLogMapper auditLogMapper;
    private final AuditService auditService;
    private final NotifyService notifyService;

    /** 每 30 秒执行一次 */
    @Scheduled(fixedDelay = 30000)
    public void run() {
        safe("唯一offer自动确认", this::autoConfirmSingleOffer);
        safe("确认逾期释放名额", this::expirePendingOffers);
        safe("公示期满正式入组", this::finishPublicityAndJoin);
        safe("环节切换通知", this::notifyStageChange);
    }

    private void safe(String name, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.warn("定时任务[{}]执行失败：{}", name, e.getMessage(), e);
        }
    }

    /** 1) 确认环节开启时，只有一个 PENDING OFFER 的学生自动确认 */
    private void autoConfirmSingleOffer() {
        if (!stageService.isOpen(Const.Stage.CONFIRM)) {
            return;
        }
        List<Offer> pending = offerMapper.selectList(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getType, Const.OfferType.OFFER)
                .eq(Offer::getStatus, Const.OfferStatus.PENDING)
                .orderByAsc(Offer::getStudentId));
        if (pending.isEmpty()) {
            return;
        }
        Map<Long, List<Offer>> groups = new LinkedHashMap<>();
        for (Offer offer : pending) {
            groups.computeIfAbsent(offer.getStudentId(), k -> new ArrayList<>()).add(offer);
        }
        int count = 0;
        for (Map.Entry<Long, List<Offer>> entry : groups.entrySet()) {
            if (entry.getValue().size() != 1) {
                continue;
            }
            if (offerService.hasConfirmedOffer(entry.getKey())) {
                continue;
            }
            offerService.autoConfirm(entry.getValue().get(0).getId());
            count++;
        }
        if (count > 0) {
            auditService.log("AUTO_CONFIRM", "offer", "自动确认 " + count + " 名学生（仅 1 个拟录取）");
        }
    }

    /** 2) 确认环节结束仍有 PENDING OFFER → EXPIRED，并按递补顺位补位 */
    private void expirePendingOffers() {
        if (!stageService.isEnded(Const.Stage.CONFIRM)) {
            return;
        }
        List<Offer> pending = offerMapper.selectList(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getType, Const.OfferType.OFFER)
                .eq(Offer::getStatus, Const.OfferStatus.PENDING));
        if (pending.isEmpty()) {
            return;
        }
        Map<String, long[]> mentorRounds = new LinkedHashMap<>();
        for (Offer offer : pending) {
            offer.setStatus(Const.OfferStatus.EXPIRED);
            offerMapper.updateById(offer);
            mentorRounds.putIfAbsent(offer.getMentorId() + "-" + offer.getRoundNo(),
                    new long[]{offer.getMentorId(), offer.getRoundNo()});
        }
        int promoted = 0;
        for (long[] item : mentorRounds.values()) {
            promoted += offerService.promoteWaitlist(item[0], (int) item[1], true);
        }
        auditService.log("AUTO_EXPIRE", "offer",
                "逾期未确认 " + pending.size() + " 条已置为 EXPIRED，递补 " + promoted + " 人");
    }

    /** 3) 公示环节结束：PUBLISHED 且无异议 → JOINED，并写正式入组记录 */
    private void finishPublicityAndJoin() {
        if (!stageService.isEnded(Const.Stage.PUBLICITY)) {
            return;
        }
        List<PublicList> rows = publicListMapper.selectList(new LambdaQueryWrapper<PublicList>()
                .eq(PublicList::getStatus, Const.PublicStatus.PUBLISHED)
                .and(w -> w.isNull(PublicList::getObjection).or().eq(PublicList::getObjection, "")));
        int count = 0;
        for (PublicList row : rows) {
            row.setStatus(Const.PublicStatus.JOINED);
            publicListMapper.updateById(row);

            SysUser student = sysUserMapper.selectById(row.getStudentId());
            if (student != null && (student.getPlaced() == null || student.getPlaced() == 0)) {
                student.setPlaced(1);
                sysUserMapper.updateById(student);
            }
            long joined = joinRecordMapper.selectCount(new LambdaQueryWrapper<JoinRecord>()
                    .eq(JoinRecord::getStudentId, row.getStudentId()));
            if (joined == 0) {
                JoinRecord record = new JoinRecord();
                record.setStudentId(row.getStudentId());
                record.setLabId(row.getLabId());
                record.setMentorId(row.getMentorId());
                record.setJoinTime(LocalDateTime.now());
                joinRecordMapper.insert(record);
            }
            count++;
        }
        if (count > 0) {
            auditService.log("AUTO_JOIN", "public_list", "公示期满，正式入组 " + count + " 人");
        }
    }

    /** 4) 环节切换通知（每个环节只写一次，用 audit_log 去重） */
    private void notifyStageChange() {
        StageWindow current = stageService.currentWindow();
        if (current == null) {
            return;
        }
        long exists = auditLogMapper.selectCount(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getAction, "STAGE_NOTICE")
                .eq(AuditLog::getTarget, current.getStageCode()));
        if (exists > 0) {
            return;
        }
        String content = "「" + current.getName() + "」环节时间窗：" + DateTimeUtil.format(current.getStartTime())
                + " 至 " + DateTimeUtil.format(current.getEndTime()) + "，请及时完成相关操作。";
        notifyService.notifyRoles(rolesOfStage(current.getStageCode()), "环节提醒：" + current.getName(), content);
        auditService.log("STAGE_NOTICE", current.getStageCode(), "已发送环节通知：" + current.getName());
    }

    /** 各环节需要通知的角色 */
    private List<String> rolesOfStage(String stageCode) {
        switch (stageCode) {
            case Const.Stage.BRIEFING:
            case Const.Stage.OPEN_DAY:
            case Const.Stage.INTENT:
                return Arrays.asList(Const.Role.STUDENT);
            case Const.Stage.INTERVIEW:
                return Arrays.asList(Const.Role.STUDENT, Const.Role.MENTOR, Const.Role.LAB_ADMIN);
            case Const.Stage.CONFIRM:
                return Arrays.asList(Const.Role.STUDENT, Const.Role.MENTOR);
            case Const.Stage.SUPPLEMENT:
                return Arrays.asList(Const.Role.STUDENT, Const.Role.MENTOR, Const.Role.LAB_ADMIN);
            case Const.Stage.TRANSFER:
                return Arrays.asList(Const.Role.COLLEGE_ADMIN, Const.Role.STUDENT);
            case Const.Stage.PUBLICITY:
                return Arrays.asList(Const.Role.STUDENT, Const.Role.MENTOR, Const.Role.LAB_ADMIN, Const.Role.COLLEGE_ADMIN);
            default:
                return Arrays.asList(Const.Role.STUDENT);
        }
    }
}
