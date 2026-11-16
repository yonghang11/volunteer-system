package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.entity.Notice;
import com.campus.volunteer.entity.SysUser;
import com.campus.volunteer.mapper.NoticeMapper;
import com.campus.volunteer.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内通知服务。
 */
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final NoticeMapper noticeMapper;
    private final SysUserMapper sysUserMapper;

    /** 给指定角色的全部用户发通知 */
    public void notifyRoles(List<String> roles, String title, String content) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getRole, roles));
        for (SysUser user : users) {
            notifyUser(user.getId(), title, content);
        }
    }

    public void notifyUser(Long userId, String title, String content) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content != null && content.length() > 512 ? content.substring(0, 512) : content);
        notice.setReadFlag(0);
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
    }
}
