package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.common.DateTimeUtil;
import com.campus.volunteer.dto.PublicExportRow;
import com.campus.volunteer.entity.Lab;
import com.campus.volunteer.entity.Mentor;
import com.campus.volunteer.entity.Offer;
import com.campus.volunteer.entity.PublicList;
import com.campus.volunteer.entity.SysUser;
import com.campus.volunteer.mapper.LabMapper;
import com.campus.volunteer.mapper.MentorMapper;
import com.campus.volunteer.mapper.OfferMapper;
import com.campus.volunteer.mapper.PublicListMapper;
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
 * 公示服务：汇总确认名单、受理异议、导出 xlsx。
 */
@Service
@RequiredArgsConstructor
public class PublicService {

    private final PublicListMapper publicListMapper;
    private final OfferMapper offerMapper;
    private final SysUserMapper sysUserMapper;
    private final LabMapper labMapper;
    private final MentorMapper mentorMapper;
    private final AuditService auditService;

    /** 公示名单 */
    public List<Map<String, Object>> list() {
        List<PublicList> rows = publicListMapper.selectList(new LambdaQueryWrapper<PublicList>()
                .orderByAsc(PublicList::getLabId)
                .orderByAsc(PublicList::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        if (rows.isEmpty()) {
            return result;
        }
        Set<Long> studentIds = new HashSet<>();
        for (PublicList row : rows) {
            studentIds.add(row.getStudentId());
        }
        Map<Long, SysUser> students = new HashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(studentIds)) {
            students.put(user.getId(), user);
        }
        Map<Long, String> labNames = new HashMap<>();
        for (Lab lab : labMapper.selectList(new LambdaQueryWrapper<Lab>())) {
            labNames.put(lab.getId(), lab.getName());
        }
        Map<Long, Mentor> mentors = new HashMap<>();
        for (Mentor mentor : mentorMapper.selectList(new LambdaQueryWrapper<Mentor>())) {
            mentors.put(mentor.getId(), mentor);
        }
        for (PublicList row : rows) {
            SysUser student = students.get(row.getStudentId());
            Mentor mentor = row.getMentorId() == null ? null : mentors.get(row.getMentorId());
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", row.getId());
            vo.put("studentId", row.getStudentId());
            vo.put("studentName", student == null ? "" : student.getRealName());
            vo.put("studentNo", student == null ? "" : student.getStudentNo());
            vo.put("classNo", student == null ? "" : student.getClassNo());
            vo.put("labId", row.getLabId());
            vo.put("labName", labNames.get(row.getLabId()));
            vo.put("mentorId", row.getMentorId());
            vo.put("mentorName", mentor == null ? "" : mentor.getName());
            vo.put("publishedAt", row.getPublishedAt());
            vo.put("objectUntil", row.getObjectUntil());
            vo.put("objection", row.getObjection());
            vo.put("status", row.getStatus());
            result.add(vo);
        }
        return result;
    }

    /** 学院管理员发布公示：汇总所有 CONFIRMED offer，公示 1 天 */
    @Transactional
    public Map<String, Object> publish() {
        CurrentUser.requireRole(Const.Role.COLLEGE_ADMIN);
        List<Offer> offers = offerMapper.selectList(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getType, Const.OfferType.OFFER)
                .eq(Offer::getStatus, Const.OfferStatus.CONFIRMED)
                .orderByAsc(Offer::getStudentId)
                .orderByAsc(Offer::getId));
        // 同一学生保留最后一条确认记录
        Map<Long, Offer> byStudent = new LinkedHashMap<>();
        for (Offer offer : offers) {
            byStudent.put(offer.getStudentId(), offer);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime objectUntil = now.plusDays(1);
        int count = 0;
        for (Offer offer : byStudent.values()) {
            PublicList row = publicListMapper.selectOne(new LambdaQueryWrapper<PublicList>()
                    .eq(PublicList::getStudentId, offer.getStudentId())
                    .orderByDesc(PublicList::getId)
                    .last("limit 1"));
            if (row != null && Const.PublicStatus.JOINED.equals(row.getStatus())) {
                continue;
            }
            if (row == null) {
                row = new PublicList();
                row.setStudentId(offer.getStudentId());
            }
            row.setLabId(offer.getLabId());
            row.setMentorId(offer.getMentorId());
            row.setPublishedAt(now);
            row.setObjectUntil(objectUntil);
            row.setStatus(Const.PublicStatus.PUBLISHED);
            if (row.getId() == null) {
                publicListMapper.insert(row);
            } else {
                publicListMapper.updateById(row);
            }
            count++;
        }
        auditService.log("PUBLIC_PUBLISH", "public_list", "发布公示名单 " + count + " 人，公示至 " + DateTimeUtil.format(objectUntil));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("publishedAt", now);
        result.put("objectUntil", objectUntil);
        return result;
    }

    /** 学生对公示提出异议 */
    @Transactional
    public void objection(String content) {
        CurrentUser.requireRole(Const.Role.STUDENT);
        if (content == null || content.trim().isEmpty()) {
            throw new BizException("请填写异议内容");
        }
        PublicList row = publicListMapper.selectOne(new LambdaQueryWrapper<PublicList>()
                .eq(PublicList::getStudentId, CurrentUser.id())
                .eq(PublicList::getStatus, Const.PublicStatus.PUBLISHED)
                .orderByDesc(PublicList::getId)
                .last("limit 1"));
        if (row == null) {
            throw new BizException("当前没有可提出异议的公示记录");
        }
        row.setStatus(Const.PublicStatus.OBJECTED);
        row.setObjection(content.length() > 255 ? content.substring(0, 255) : content);
        publicListMapper.updateById(row);
        auditService.log("PUBLIC_OBJECTION", "public:" + row.getId(), "学生 " + CurrentUser.id() + " 提出异议：" + content);
    }

    /** 导出数据 */
    public List<PublicExportRow> exportRows() {
        List<Map<String, Object>> list = list();
        List<PublicExportRow> rows = new ArrayList<>();
        int index = 1;
        for (Map<String, Object> item : list) {
            PublicExportRow row = new PublicExportRow();
            row.setIndex(index++);
            row.setStudentNo(String.valueOf(item.get("studentNo")));
            row.setStudentName(String.valueOf(item.get("studentName")));
            row.setLabName(String.valueOf(item.get("labName")));
            row.setMentorName(String.valueOf(item.get("mentorName")));
            Object publishedAt = item.get("publishedAt");
            row.setPublishedAt(publishedAt == null ? "" : DateTimeUtil.format((LocalDateTime) publishedAt));
            row.setStatus(statusText(String.valueOf(item.get("status"))));
            rows.add(row);
        }
        return rows;
    }

    private String statusText(String status) {
        if (Const.PublicStatus.PUBLISHED.equals(status)) {
            return "公示中";
        }
        if (Const.PublicStatus.OBJECTED.equals(status)) {
            return "有异议";
        }
        if (Const.PublicStatus.JOINED.equals(status)) {
            return "已入组";
        }
        return status;
    }
}
