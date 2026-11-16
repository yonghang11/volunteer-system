package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.entity.AuditLog;
import com.campus.volunteer.entity.Offer;
import com.campus.volunteer.entity.SysUser;
import com.campus.volunteer.mapper.AuditLogMapper;
import com.campus.volunteer.mapper.IntentMapper;
import com.campus.volunteer.mapper.InterviewMapper;
import com.campus.volunteer.mapper.OfferMapper;
import com.campus.volunteer.mapper.ScoreMapper;
import com.campus.volunteer.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台统计服务。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final SysUserMapper sysUserMapper;
    private final IntentMapper intentMapper;
    private final InterviewMapper interviewMapper;
    private final ScoreMapper scoreMapper;
    private final OfferMapper offerMapper;
    private final AuditLogMapper auditLogMapper;
    private final QuotaService quotaService;

    /** 全站统计概览（学院管理员/实训室管理员） */
    public Map<String, Object> overview() {
        com.campus.volunteer.common.CurrentUser.requireRole(Const.Role.COLLEGE_ADMIN, Const.Role.LAB_ADMIN);
        long studentCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, Const.Role.STUDENT));
        long intentCount = intentMapper.selectCount(null);
        long interviewCount = interviewMapper.selectCount(null);
        long scoredCount = scoreMapper.selectCount(null);
        long offerCount = offerMapper.selectCount(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getType, Const.OfferType.OFFER));
        long confirmedCount = offerMapper.selectCount(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getType, Const.OfferType.OFFER)
                .eq(Offer::getStatus, Const.OfferStatus.CONFIRMED));
        Set<Long> confirmedStudents = new HashSet<>();
        for (Offer offer : offerMapper.selectList(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getType, Const.OfferType.OFFER)
                .eq(Offer::getStatus, Const.OfferStatus.CONFIRMED))) {
            confirmedStudents.add(offer.getStudentId());
        }
        long unplacedCount = studentCount - confirmedStudents.size();
        int round = quotaService.currentRound();
        long remainQuota = 0;
        for (Map<String, Object> slot : quotaService.slots(round)) {
            remainQuota += (int) slot.get("remain");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("round", round);
        result.put("studentCount", studentCount);
        result.put("intentCount", intentCount);
        result.put("interviewCount", interviewCount);
        result.put("scoredCount", scoredCount);
        result.put("offerCount", offerCount);
        result.put("confirmedCount", confirmedCount);
        result.put("unplacedCount", unplacedCount);
        result.put("remainQuota", remainQuota);
        return result;
    }

    /** 最近 50 条审计日志（学院管理员） */
    public List<AuditLog> recentAudits() {
        com.campus.volunteer.common.CurrentUser.requireRole(Const.Role.COLLEGE_ADMIN);
        return auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
                .orderByDesc(AuditLog::getCreateTime)
                .orderByDesc(AuditLog::getId)
                .last("limit 50"));
    }
}
