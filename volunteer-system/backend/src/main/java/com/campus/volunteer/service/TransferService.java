package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.dto.TransferAssignReq;
import com.campus.volunteer.dto.TransferRefuseReq;
import com.campus.volunteer.entity.Intent;
import com.campus.volunteer.entity.Lab;
import com.campus.volunteer.entity.Mentor;
import com.campus.volunteer.entity.SysUser;
import com.campus.volunteer.entity.TransferTask;
import com.campus.volunteer.mapper.IntentMapper;
import com.campus.volunteer.mapper.LabMapper;
import com.campus.volunteer.mapper.MentorMapper;
import com.campus.volunteer.mapper.SysUserMapper;
import com.campus.volunteer.mapper.TransferTaskMapper;
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
 * 统筹调剂：优先级 1 实训室内部调剂、2 全院跨实训室、3 约谈未服从学生后强制安置。
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferTaskMapper transferTaskMapper;
    private final SysUserMapper sysUserMapper;
    private final IntentMapper intentMapper;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final OfferService offerService;
    private final QuotaService quotaService;
    private final AuditService auditService;

    /** 调剂工作台：未录取学生、实时名额、调剂任务 */
    public Map<String, Object> pool() {
        CurrentUser.requireRole(Const.Role.COLLEGE_ADMIN);
        int round = quotaService.currentRound();
        List<Map<String, Object>> unplaced = new ArrayList<>();
        for (SysUser student : unplacedStudents()) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("studentId", student.getId());
            vo.put("name", student.getRealName());
            vo.put("studentNo", student.getStudentNo());
            vo.put("classNo", student.getClassNo());
            unplaced.add(vo);
        }
        List<Map<String, Object>> tasks = taskRows();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("round", round);
        result.put("unplaced", unplaced);
        result.put("slots", quotaService.slots(round));
        result.put("tasks", tasks);
        return result;
    }

    /** 自动生成调剂任务（重复调用先清掉 PENDING 的旧任务） */
    @Transactional
    public Map<String, Object> auto() {
        CurrentUser.requireRole(Const.Role.COLLEGE_ADMIN);
        int round = quotaService.currentRound();
        transferTaskMapper.delete(new LambdaQueryWrapper<TransferTask>()
                .eq(TransferTask::getStatus, Const.TaskStatus.PENDING));

        // 实时名额视图
        List<Map<String, Object>> slots = quotaService.slots(round);
        Map<Long, Boolean> labHasRemain = new HashMap<>();
        boolean anyRemain = false;
        for (Map<String, Object> slot : slots) {
            int remain = (int) slot.get("remain");
            Long labId = (Long) slot.get("labId");
            if (remain > 0) {
                labHasRemain.put(labId, true);
                anyRemain = true;
            } else {
                labHasRemain.putIfAbsent(labId, false);
            }
        }

        int count = 0;
        for (SysUser student : unplacedStudents()) {
            List<Intent> intents = intentMapper.selectList(new LambdaQueryWrapper<Intent>()
                    .eq(Intent::getStudentId, student.getId())
                    .orderByDesc(Intent::getRoundNo)
                    .orderByAsc(Intent::getId));
            int maxRound = 0;
            boolean acceptCross = false;
            Set<Long> labIds = new HashSet<>();
            for (Intent intent : intents) {
                if (intent.getRoundNo() > maxRound) {
                    maxRound = intent.getRoundNo();
                    labIds.clear();
                    acceptCross = false;
                }
                if (intent.getRoundNo() == maxRound) {
                    labIds.add(intent.getLabId());
                    if (intent.getAcceptCross() != null && intent.getAcceptCross() == 1) {
                        acceptCross = true;
                    }
                }
            }

            int priority = 3;
            Long fromLabId = null;
            for (Long labId : labIds) {
                if (Boolean.TRUE.equals(labHasRemain.get(labId))) {
                    priority = 1;
                    fromLabId = labId;
                    break;
                }
            }
            if (priority != 1 && acceptCross && anyRemain) {
                priority = 2;
                fromLabId = labIds.isEmpty() ? null : labIds.iterator().next();
            }

            TransferTask task = new TransferTask();
            task.setStudentId(student.getId());
            task.setPriority(priority);
            task.setFromLabId(fromLabId);
            task.setStatus(Const.TaskStatus.PENDING);
            task.setCreateTime(LocalDateTime.now());
            transferTaskMapper.insert(task);
            count++;
        }
        auditService.log("TRANSFER_AUTO", "round:" + round, "生成调剂任务 " + count + " 条");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("round", round);
        result.put("count", count);
        return result;
    }

    /** 指派导师：生成 CONFIRMED offer + 意向确认，任务置 ASSIGNED */
    @Transactional
    public void assign(Long taskId, TransferAssignReq req) {
        CurrentUser.requireRole(Const.Role.COLLEGE_ADMIN);
        if (req == null || req.getMentorId() == null) {
            throw new BizException("请选择接收导师");
        }
        TransferTask task = transferTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("调剂任务不存在");
        }
        if (Const.TaskStatus.ASSIGNED.equals(task.getStatus())) {
            throw new BizException("该任务已完成指派");
        }
        Mentor mentor = mentorMapper.selectById(req.getMentorId());
        if (mentor == null) {
            throw new BizException("导师不存在：" + req.getMentorId());
        }
        int round = quotaService.currentRound();
        if (quotaService.remainOfMentor(mentor.getId(), round) <= 0) {
            throw new BizException("该导师名额已满，请选择其他导师");
        }
        offerService.upsertOffer(task.getStudentId(), mentor.getId(), mentor.getLabId(), round,
                Const.OfferType.OFFER, Const.OfferStatus.CONFIRMED, null);
        offerService.declineOtherPendingOffers(task.getStudentId(), null);
        offerService.updateIntentStatus(task.getStudentId(), mentor.getId(), round, Const.IntentStatus.CONFIRMED);

        Intent intent = intentMapper.selectOne(new LambdaQueryWrapper<Intent>()
                .eq(Intent::getStudentId, task.getStudentId())
                .eq(Intent::getMentorId, mentor.getId())
                .eq(Intent::getRoundNo, round));
        if (intent == null) {
            intent = new Intent();
            intent.setStudentId(task.getStudentId());
            intent.setMentorId(mentor.getId());
            intent.setLabId(mentor.getLabId());
            intent.setRoundNo(round);
            intent.setAcceptInner(1);
            intent.setAcceptCross(1);
            intent.setStatus(Const.IntentStatus.CONFIRMED);
            intent.setCreateTime(LocalDateTime.now());
            intent.setUpdateTime(LocalDateTime.now());
            intentMapper.insert(intent);
        }

        task.setStatus(Const.TaskStatus.ASSIGNED);
        task.setToMentorId(mentor.getId());
        task.setToLabId(mentor.getLabId());
        task.setOperator("COLLEGE_ADMIN#" + CurrentUser.id());
        task.setTalkNote(req.getNote());
        transferTaskMapper.updateById(task);
        auditService.log("TRANSFER_ASSIGN", "task:" + taskId,
                "学生 " + task.getStudentId() + " 指派至导师 " + mentor.getId() + "（" + mentor.getName() + "）");
    }

    /** 学生拒绝安置（约谈后仍不服从，由学院强制安置） */
    @Transactional
    public void refuse(Long taskId, TransferRefuseReq req) {
        CurrentUser.requireRole(Const.Role.COLLEGE_ADMIN);
        TransferTask task = transferTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("调剂任务不存在");
        }
        if (Const.TaskStatus.ASSIGNED.equals(task.getStatus())) {
            throw new BizException("该任务已完成指派，不能拒绝");
        }
        task.setStatus(Const.TaskStatus.REFUSED);
        task.setTalkNote(req == null ? null : req.getNote());
        task.setOperator("COLLEGE_ADMIN#" + CurrentUser.id());
        transferTaskMapper.updateById(task);
        auditService.log("TRANSFER_REFUSE", "task:" + taskId,
                "学生 " + task.getStudentId() + " 拒绝安置，备注：" + (req == null ? "" : req.getNote()));
    }

    /** 未录取学生：未正式入组且没有确认录取 */
    public List<SysUser> unplacedStudents() {
        List<SysUser> students = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, Const.Role.STUDENT)
                .orderByAsc(SysUser::getId));
        List<SysUser> result = new ArrayList<>();
        for (SysUser student : students) {
            if (student.getPlaced() != null && student.getPlaced() == 1) {
                continue;
            }
            if (offerService.hasConfirmedOffer(student.getId())) {
                continue;
            }
            result.add(student);
        }
        return result;
    }

    /** 任务列表（含姓名与实训室名称） */
    private List<Map<String, Object>> taskRows() {
        List<TransferTask> tasks = transferTaskMapper.selectList(new LambdaQueryWrapper<TransferTask>()
                .orderByAsc(TransferTask::getPriority)
                .orderByAsc(TransferTask::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        if (tasks.isEmpty()) {
            return rows;
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> mentorIds = new HashSet<>();
        Set<Long> labIds = new HashSet<>();
        for (TransferTask task : tasks) {
            userIds.add(task.getStudentId());
            if (task.getToMentorId() != null) {
                mentorIds.add(task.getToMentorId());
            }
            if (task.getFromLabId() != null) {
                labIds.add(task.getFromLabId());
            }
            if (task.getToLabId() != null) {
                labIds.add(task.getToLabId());
            }
        }
        Map<Long, SysUser> students = new HashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(userIds)) {
            students.put(user.getId(), user);
        }
        Map<Long, Mentor> mentors = new HashMap<>();
        if (!mentorIds.isEmpty()) {
            for (Mentor mentor : mentorMapper.selectBatchIds(mentorIds)) {
                mentors.put(mentor.getId(), mentor);
            }
        }
        Map<Long, String> labNames = new HashMap<>();
        if (!labIds.isEmpty()) {
            for (Lab lab : labMapper.selectBatchIds(labIds)) {
                labNames.put(lab.getId(), lab.getName());
            }
        }
        for (TransferTask task : tasks) {
            SysUser student = students.get(task.getStudentId());
            Mentor mentor = task.getToMentorId() == null ? null : mentors.get(task.getToMentorId());
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", task.getId());
            vo.put("studentId", task.getStudentId());
            vo.put("studentName", student == null ? "" : student.getRealName());
            vo.put("studentNo", student == null ? "" : student.getStudentNo());
            vo.put("priority", task.getPriority());
            vo.put("fromLabId", task.getFromLabId());
            vo.put("fromLabName", labNames.get(task.getFromLabId()));
            vo.put("toLabId", task.getToLabId());
            vo.put("toLabName", labNames.get(task.getToLabId()));
            vo.put("toMentorId", task.getToMentorId());
            vo.put("toMentorName", mentor == null ? null : mentor.getName());
            vo.put("status", task.getStatus());
            vo.put("note", task.getTalkNote());
            vo.put("operator", task.getOperator());
            vo.put("createTime", task.getCreateTime());
            rows.add(vo);
        }
        return rows;
    }
}
