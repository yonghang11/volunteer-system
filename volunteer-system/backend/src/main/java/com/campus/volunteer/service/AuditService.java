package com.campus.volunteer.service;

import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.entity.AuditLog;
import com.campus.volunteer.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审计日志服务：所有关键动作统一留痕。
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    public void log(String action, String target, String detail) {
        AuditLog log = new AuditLog();
        CurrentUser user = CurrentUser.optional();
        log.setActor(user == null ? "system" : (user.getRole() + "#" + user.getId()));
        log.setAction(action);
        log.setTarget(truncate(target, 128));
        log.setDetail(truncate(detail, 512));
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
