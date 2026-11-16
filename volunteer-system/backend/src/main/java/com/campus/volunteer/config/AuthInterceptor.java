package com.campus.volunteer.config;

import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.common.R;
import com.campus.volunteer.common.TokenUtil;
import com.campus.volunteer.entity.SysUser;
import com.campus.volunteer.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 鉴权拦截器：解析 Authorization: Bearer xxx，写入 ThreadLocal。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenUtil tokenUtil;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(response);
        }
        TokenUtil.Claims claims = tokenUtil.verify(header.substring(7).trim());
        if (claims == null) {
            return reject(response);
        }
        SysUser user = sysUserMapper.selectById(claims.getUserId());
        if (user == null) {
            return reject(response);
        }
        CurrentUser.set(new CurrentUser(user.getId(), user.getRole(), user.getLabId()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }

    /** 未登录/令牌失效：HTTP 200 + code=401 */
    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(401, "登录已失效，请重新登录")));
        return false;
    }
}
