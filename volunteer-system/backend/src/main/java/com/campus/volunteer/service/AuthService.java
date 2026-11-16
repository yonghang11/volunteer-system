package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.common.DigestUtil;
import com.campus.volunteer.common.TokenUtil;
import com.campus.volunteer.dto.LoginReq;
import com.campus.volunteer.entity.SysUser;
import com.campus.volunteer.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录认证服务：密码为 SHA-256 十六进制摘要，登录成功后签发自制 token。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final TokenUtil tokenUtil;
    private final AuditService auditService;

    @Transactional
    public Map<String, Object> login(LoginReq req) {
        if (req == null || req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            throw new BizException("请输入用户名");
        }
        if (req.getPassword() == null || req.getPassword().isEmpty()) {
            throw new BizException("请输入密码");
        }
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername().trim()));
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }
        String digest = DigestUtil.sha256Hex(req.getPassword());
        if (!digest.equalsIgnoreCase(user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        String token = tokenUtil.create(user.getId(), user.getRole());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", userVo(user));
        auditService.log("LOGIN", "user:" + user.getId(), user.getUsername() + " 登录成功");
        return result;
    }

    /** 当前登录用户信息 */
    public Map<String, Object> me() {
        SysUser user = sysUserMapper.selectById(CurrentUser.id());
        if (user == null) {
            throw new BizException(401, "登录已失效，请重新登录");
        }
        return userVo(user);
    }

    private Map<String, Object> userVo(SysUser user) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", user.getId());
        vo.put("username", user.getUsername());
        vo.put("realName", user.getRealName());
        vo.put("role", user.getRole());
        vo.put("labId", user.getLabId());
        vo.put("studentNo", user.getStudentNo());
        vo.put("classNo", user.getClassNo());
        vo.put("placed", user.getPlaced());
        vo.put("bioUrl", user.getBioUrl());
        vo.put("bioName", user.getBioName());
        vo.put("bioUpdatedAt", user.getBioUpdatedAt());
        return vo;
    }
}
