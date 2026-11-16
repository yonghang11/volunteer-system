package com.campus.volunteer.dto;

import lombok.Data;

/** 登录请求 */
@Data
public class LoginReq {

    private String username;

    private String password;
}
