package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.dto.InterviewScoreReq;
import com.campus.volunteer.entity.Interview;
import com.campus.volunteer.entity.Intent;
import com.campus.volunteer.entity.Lab;
import com.campus.volunteer.entity.Mentor;
import com.campus.volunteer.entity.Score;
import com.campus.volunteer.entity.StageWindow;
import com.campus.volunteer.entity.SysUser;
import com.campus.volunteer.mapper.InterviewMapper;
import com.campus.volunteer.mapper.IntentMapper;
import com.campus.volunteer.mapper.LabMapper;
import com.campus.volunteer.mapper.MentorMapper;
import com.campus.volunteer.mapper.ScoreMapper;
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
 * 面试服务：以实训室为单位统一安排，导师四项评分。
 */
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewMapper interviewMapper;
    private final IntentMapper intentMapper;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final SysUserMapper sysUserMapper;
    private final ScoreMapper scoreMapper;
    private final StageService stageService;
    private final QuotaService quotaService;
    private final MentorService mentorService;
    private final AuditService auditService;

    /** 学生查看自己的面试安排 */
    public List<Map<String, Object>> my() {
        CurrentUser.requireRole(Const.Role.STUDENT);
        Long studentId = CurrentUser.id();
        List<Interview> interviews = interviewMapper.selectList(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getStudentId, studentId)
                .orderByAsc(Interview::getSlotStart));
        return buildRows(interviews);
    }

    /** 实训室管理员为本室该轮意向学生生成面试安排 */
    @Transactional
    public Map<String, Object> generate(Integer round) {
        CurrentUser.requireRole(Const.Role.LAB_ADMIN);
        Long labId = CurrentUser.labId();
        if (labId == null) {
            throw new BizException("当前管理员账号未绑定实训室");
        }
        int r = round == null ? quotaService.currentRound() : round;
        Lab lab = labMapper.selectById(labId);
        String labName = lab == null ? "实训室" : lab.getName();

        List<Intent> intents = intentMapper.selectList(new LambdaQueryWrapper<Intent>()
                .eq(Intent::getLabId, labId)
                .eq(Intent::getRoundNo, r)
                .orderByAsc(Intent::getStudentId)
                .orderByAsc(Intent::getId));
        // 按学生去重，取第一个意向导师
        Map<Long, Long> studentMentor = new LinkedHashMap<>();
        for (Intent intent : intents) {
            studentMentor.putIfAbsent(intent.getStudentId(), intent.getMentorId());
        }

        LocalDateTime base = interviewBaseTime();
        String place = labName + "会议室";
        int created = 0;
        int index = 0;
        for (Map.Entry<Long, Long> entry : studentMentor.entrySet()) {
            long exists = interviewMapper.selectCount(new LambdaQueryWrapper<Interview>()
                    .eq(Interview::getStudentId, entry.getKey())
                    .eq(Interview::getRoundNo, r));
            if (exists > 0) {
                continue;
            }
            Interview interview = new Interview();
            interview.setLabId(labId);
            interview.setStudentId(entry.getKey());
            interview.setMentorId(entry.getValue());
            interview.setRoundNo(r);
            interview.setSlotStart(base.plusMinutes(20L * index));
            interview.setPlace(place);
            interview.setStatus(Const.InterviewStatus.PENDING);
            interviewMapper.insert(interview);
            created++;
            index++;
        }
        auditService.log("INTERVIEW_GENERATE", "lab:" + labId,
                "第" + r + "轮生成面试 " + created + " 场，起始时间 " + base);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("round", r);
        result.put("created", created);
        result.put("total", studentMentor.size());
        return result;
    }

    /** 面试起始时间：取面试环节开始时间，取不到则用当前时间 */
    private LocalDateTime interviewBaseTime() {
        StageWindow window;
        try {
            window = stageService.window(Const.Stage.INTERVIEW);
        } catch (BizException e) {
            return LocalDateTime.now();
        }
        return window.getStartTime() == null ? LocalDateTime.now() : window.getStartTime();
    }

    /** 实训室管理员/导师查看本室（本人）候选人及评分 */
    public List<Map<String, Object>> labView(Integer round) {
        String role = CurrentUser.role();
        if (!Const.Role.LAB_ADMIN.equals(role) && !Const.Role.MENTOR.equals(role)) {
            throw new BizException(403, "无权访问该接口，需要角色：LAB_ADMIN/MENTOR");
        }
        int r = round == null ? quotaService.currentRound() : round;
        Long labId = CurrentUser.labId();
        Set<Long> studentFilter = null;
        if (Const.Role.MENTOR.equals(role)) {
            Mentor mentor = mentorService.current();
            labId = mentor.getLabId();
            List<Intent> intents = intentMapper.selectList(new LambdaQueryWrapper<Intent>()
                    .eq(Intent::getMentorId, mentor.getId())
                    .eq(Intent::getRoundNo, r));
            studentFilter = new HashSet<>();
            for (Intent intent : intents) {
                studentFilter.add(intent.getStudentId());
            }
            if (studentFilter.isEmpty()) {
                return new ArrayList<>();
            }
        }
        if (labId == null) {
            throw new BizException("当前账号未绑定实训室");
        }
        List<Interview> interviews = interviewMapper.selectList(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getLabId, labId)
                .eq(Interview::getRoundNo, r)
                .orderByAsc(Interview::getSlotStart)
                .orderByAsc(Interview::getId));
        List<Interview> filtered = new ArrayList<>();
        for (Interview interview : interviews) {
            if (studentFilter == null || studentFilter.contains(interview.getStudentId())) {
                filtered.add(interview);
            }
        }
        return buildRows(filtered);
    }

    /** 导师评分：四项 0-25，总分后端求和，并置面试状态为 DONE */
    @Transactional
    public Map<String, Object> score(Long interviewId, InterviewScoreReq req) {
        CurrentUser.requireRole(Const.Role.MENTOR);
        Mentor mentor = mentorService.current();
        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new BizException("面试安排不存在");
        }
        if (!mentor.getLabId().equals(interview.getLabId())) {
            throw new BizException("只能为本实训室的候选人评分");
        }
        checkScore("学习态度", req.getAttitude());
        checkScore("逻辑思维", req.getLogic());
        checkScore("编程基础", req.getCoding());
        checkScore("时间投入", req.getCommitment());
        String result = req.getResult();
        if (result == null || !Const.Result.OFFER.equals(result) && !Const.Result.WAITLIST.equals(result)
                && !Const.Result.INNER.equals(result) && !Const.Result.REJECT.equals(result)) {
            throw new BizException("面试结论不合法，应为 OFFER/WAITLIST/INNER/REJECT");
        }
        int total = req.getAttitude() + req.getLogic() + req.getCoding() + req.getCommitment();

        Score score = scoreMapper.selectOne(new LambdaQueryWrapper<Score>()
                .eq(Score::getInterviewId, interviewId));
        if (score == null) {
            score = new Score();
            score.setInterviewId(interviewId);
            score.setStudentId(interview.getStudentId());
            score.setMentorId(mentor.getId());
            score.setLabId(interview.getLabId());
            score.setRoundNo(interview.getRoundNo());
        }
        score.setAttitude(req.getAttitude());
        score.setLogic(req.getLogic());
        score.setCoding(req.getCoding());
        score.setCommitment(req.getCommitment());
        score.setTotal(total);
        score.setResult(result);
        score.setRemark(req.getRemark());
        score.setScorer(CurrentUser.id());
        score.setScoreTime(LocalDateTime.now());
        if (score.getId() == null) {
            scoreMapper.insert(score);
        } else {
            scoreMapper.updateById(score);
        }

        interview.setStatus(Const.InterviewStatus.DONE);
        interviewMapper.updateById(interview);
        auditService.log("INTERVIEW_SCORE", "interview:" + interviewId,
                "总分 " + total + "，结论 " + result + "，评分人 " + mentor.getName());

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("interviewId", interviewId);
        vo.put("total", total);
        vo.put("result", result);
        return vo;
    }

    private void checkScore(String label, Integer value) {
        if (value == null) {
            throw new BizException(label + "评分必填");
        }
        if (value < 0 || value > 25) {
            throw new BizException(label + "评分必须在 0-25 之间");
        }
    }

    /** 组装面试展示数据（含评分与学生信息） */
    private List<Map<String, Object>> buildRows(List<Interview> interviews) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (interviews.isEmpty()) {
            return rows;
        }
        Set<Long> studentIds = new HashSet<>();
        Set<Long> mentorIds = new HashSet<>();
        Set<Long> labIds = new HashSet<>();
        Set<Long> interviewIds = new HashSet<>();
        for (Interview interview : interviews) {
            studentIds.add(interview.getStudentId());
            labIds.add(interview.getLabId());
            interviewIds.add(interview.getId());
            if (interview.getMentorId() != null) {
                mentorIds.add(interview.getMentorId());
            }
        }
        Map<Long, SysUser> students = new HashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(studentIds)) {
            students.put(user.getId(), user);
        }
        Map<Long, Mentor> mentors = new HashMap<>();
        if (!mentorIds.isEmpty()) {
            for (Mentor mentor : mentorMapper.selectBatchIds(mentorIds)) {
                mentors.put(mentor.getId(), mentor);
            }
        }
        Map<Long, String> labNames = new HashMap<>();
        for (Lab lab : labMapper.selectBatchIds(labIds)) {
            labNames.put(lab.getId(), lab.getName());
        }
        Map<Long, Score> scores = new HashMap<>();
        for (Score score : scoreMapper.selectList(new LambdaQueryWrapper<Score>()
                .in(Score::getInterviewId, interviewIds))) {
            scores.put(score.getInterviewId(), score);
        }
        for (Interview interview : interviews) {
            SysUser student = students.get(interview.getStudentId());
            Mentor mentor = interview.getMentorId() == null ? null : mentors.get(interview.getMentorId());
            Score score = scores.get(interview.getId());
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", interview.getId());
            vo.put("interviewId", interview.getId());
            vo.put("studentId", interview.getStudentId());
            vo.put("studentName", student == null ? "" : student.getRealName());
            vo.put("studentNo", student == null ? "" : student.getStudentNo());
            vo.put("classNo", student == null ? "" : student.getClassNo());
            vo.put("labId", interview.getLabId());
            vo.put("labName", labNames.get(interview.getLabId()));
            vo.put("mentorId", interview.getMentorId());
            vo.put("mentorName", mentor == null ? "" : mentor.getName());
            vo.put("roundNo", interview.getRoundNo());
            vo.put("slotStart", interview.getSlotStart());
            vo.put("place", interview.getPlace());
            vo.put("status", interview.getStatus());
            vo.put("attitude", score == null ? null : score.getAttitude());
            vo.put("logic", score == null ? null : score.getLogic());
            vo.put("coding", score == null ? null : score.getCoding());
            vo.put("commitment", score == null ? null : score.getCommitment());
            vo.put("total", score == null ? null : score.getTotal());
            vo.put("result", score == null ? null : score.getResult());
            vo.put("remark", score == null ? null : score.getRemark());
            rows.add(vo);
        }
        return rows;
    }
}
