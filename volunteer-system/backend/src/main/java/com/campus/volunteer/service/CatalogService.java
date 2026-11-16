package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.entity.Lab;
import com.campus.volunteer.entity.Mentor;
import com.campus.volunteer.entity.SessionInfo;
import com.campus.volunteer.mapper.LabMapper;
import com.campus.volunteer.mapper.MentorMapper;
import com.campus.volunteer.mapper.SessionInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 目录服务：实训室（含导师）与宣讲/开放日排期。
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final LabMapper labMapper;
    private final MentorMapper mentorMapper;
    private final SessionInfoMapper sessionInfoMapper;
    private final QuotaService quotaService;

    /** 实训室列表，每个实训室含 mentors 子数组，均带 quota/remain */
    public List<Map<String, Object>> labs() {
        int round = quotaService.currentRound();
        List<Lab> labs = labMapper.selectList(new LambdaQueryWrapper<Lab>().orderByAsc(Lab::getId));
        List<Mentor> mentors = mentorMapper.selectList(new LambdaQueryWrapper<Mentor>()
                .orderByAsc(Mentor::getLabId)
                .orderByAsc(Mentor::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Lab lab : labs) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", lab.getId());
            vo.put("code", lab.getCode());
            vo.put("name", lab.getName());
            vo.put("direction", lab.getDirection());
            vo.put("intro", lab.getIntro());
            vo.put("quota", lab.getQuotaTotal());
            vo.put("remain", Math.max(0, lab.getQuotaTotal() - (int) quotaService.usedByLab(lab.getId(), round)));
            List<Map<String, Object>> mentorVos = new ArrayList<>();
            for (Mentor mentor : mentors) {
                if (!mentor.getLabId().equals(lab.getId())) {
                    continue;
                }
                Map<String, Object> mv = new LinkedHashMap<>();
                mv.put("id", mentor.getId());
                mv.put("name", mentor.getName());
                mv.put("title", mentor.getTitle());
                mv.put("direction", mentor.getDirection());
                mv.put("intro", mentor.getIntro());
                mv.put("quota", mentor.getQuota());
                mv.put("remain", Math.max(0, mentor.getQuota() - (int) quotaService.usedByMentor(mentor.getId(), round)));
                mv.put("acceptCrossTransfer", mentor.getAcceptCrossTransfer());
                mentorVos.add(mv);
            }
            vo.put("mentors", mentorVos);
            result.add(vo);
        }
        return result;
    }

    /** 宣讲与开放日 */
    public List<Map<String, Object>> sessions() {
        List<SessionInfo> sessions = sessionInfoMapper.selectList(new LambdaQueryWrapper<SessionInfo>()
                .orderByAsc(SessionInfo::getStartTime)
                .orderByAsc(SessionInfo::getId));
        List<Lab> labs = labMapper.selectList(new LambdaQueryWrapper<Lab>());
        Map<Long, String> labNames = new LinkedHashMap<>();
        for (Lab lab : labs) {
            labNames.put(lab.getId(), lab.getName());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (SessionInfo s : sessions) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", s.getId());
            vo.put("type", s.getType());
            vo.put("labId", s.getLabId());
            vo.put("labName", s.getLabId() == null ? "学院" : labNames.get(s.getLabId()));
            vo.put("title", s.getTitle());
            vo.put("startTime", s.getStartTime());
            vo.put("endTime", s.getEndTime());
            vo.put("place", s.getPlace());
            vo.put("capacity", s.getCapacity());
            vo.put("content", s.getContent());
            result.add(vo);
        }
        return result;
    }
}
