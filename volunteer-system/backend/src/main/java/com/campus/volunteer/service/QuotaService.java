package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.entity.Lab;
import com.campus.volunteer.entity.Mentor;
import com.campus.volunteer.entity.Offer;
import com.campus.volunteer.entity.SupplementRound;
import com.campus.volunteer.mapper.LabMapper;
import com.campus.volunteer.mapper.MentorMapper;
import com.campus.volunteer.mapper.OfferMapper;
import com.campus.volunteer.mapper.SupplementRoundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 名额计算服务：remain = quota - 本 round 已占用（PENDING/CONFIRMED）的 OFFER 数。
 */
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final OfferMapper offerMapper;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final SupplementRoundMapper supplementRoundMapper;

    /** 当前轮次：已开启的最大补录轮次，否则第 1 轮 */
    public int currentRound() {
        List<SupplementRound> rounds = supplementRoundMapper.selectList(new LambdaQueryWrapper<SupplementRound>()
                .eq(SupplementRound::getOpenFlag, 1)
                .orderByDesc(SupplementRound::getRoundNo));
        return rounds.isEmpty() ? 1 : rounds.get(0).getRoundNo();
    }

    /** 某导师某轮已占用的 OFFER 数 */
    public long usedByMentor(Long mentorId, int round) {
        return offerMapper.selectCount(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getMentorId, mentorId)
                .eq(Offer::getRoundNo, round)
                .eq(Offer::getType, Const.OfferType.OFFER)
                .in(Offer::getStatus, Const.OfferStatus.PENDING, Const.OfferStatus.CONFIRMED));
    }

    /** 某实训室某轮已占用的 OFFER 数 */
    public long usedByLab(Long labId, int round) {
        return offerMapper.selectCount(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getLabId, labId)
                .eq(Offer::getRoundNo, round)
                .eq(Offer::getType, Const.OfferType.OFFER)
                .in(Offer::getStatus, Const.OfferStatus.PENDING, Const.OfferStatus.CONFIRMED));
    }

    /** 某导师剩余名额 */
    public int remainOfMentor(Long mentorId, int round) {
        Mentor mentor = mentorMapper.selectById(mentorId);
        if (mentor == null) {
            return 0;
        }
        return Math.max(0, mentor.getQuota() - (int) usedByMentor(mentorId, round));
    }

    /** 全部导师名额视图（含实时 remain） */
    public List<Map<String, Object>> slots(int round) {
        List<Mentor> mentors = mentorMapper.selectList(new LambdaQueryWrapper<Mentor>()
                .orderByAsc(Mentor::getLabId)
                .orderByAsc(Mentor::getId));
        List<Lab> labs = labMapper.selectList(new LambdaQueryWrapper<Lab>().orderByAsc(Lab::getId));
        Map<Long, String> labNames = new LinkedHashMap<>();
        for (Lab lab : labs) {
            labNames.put(lab.getId(), lab.getName());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Mentor mentor : mentors) {
            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("mentorId", mentor.getId());
            slot.put("mentorName", mentor.getName());
            slot.put("labId", mentor.getLabId());
            slot.put("labName", labNames.get(mentor.getLabId()));
            slot.put("quota", mentor.getQuota());
            slot.put("remain", remainOfMentor(mentor.getId(), round));
            result.add(slot);
        }
        return result;
    }

    /**
     * 当前登录导师本人的名额概况（供导师评审页使用）。
     */
    public Map<String, Object> myMentorQuota(Integer round) {
        CurrentUser.requireRole(Const.Role.MENTOR);
        Mentor mentor = mentorMapper.selectOne(new LambdaQueryWrapper<Mentor>()
                .eq(Mentor::getUserId, CurrentUser.id())
                .last("limit 1"));
        if (mentor == null) {
            throw new BizException("当前账号未绑定导师信息，请联系学院管理员");
        }
        int r = (round == null) ? currentRound() : round;
        Lab lab = labMapper.selectById(mentor.getLabId());
        long used = usedByMentor(mentor.getId(), r);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mentorId", mentor.getId());
        data.put("mentorName", mentor.getName());
        data.put("labId", mentor.getLabId());
        data.put("labName", lab == null ? null : lab.getName());
        data.put("round", r);
        data.put("quota", mentor.getQuota());
        data.put("used", used);
        data.put("remain", Math.max(0, mentor.getQuota() - (int) used));
        data.put("waitlistCount", countByType(mentor.getId(), r, Const.OfferType.WAITLIST));
        data.put("innerCount", countByType(mentor.getId(), r, Const.OfferType.INNER));
        return data;
    }

    /** 某导师某轮某类型（递补/内部调剂候选）待处理数量 */
    private long countByType(Long mentorId, int round, String type) {
        return offerMapper.selectCount(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getMentorId, mentorId)
                .eq(Offer::getRoundNo, round)
                .eq(Offer::getType, type)
                .eq(Offer::getStatus, Const.OfferStatus.PENDING));
    }
}
