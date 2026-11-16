package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统用户 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    /** STUDENT/MENTOR/LAB_ADMIN/COLLEGE_ADMIN */
    private String role;

    private String studentNo;

    private String classNo;

    private String phone;

    /** 个人简介文件访问URL */
    private String bioUrl;

    /** 个人简介原始文件名 */
    private String bioName;

    /** 个人简介最后更新时间 */
    private LocalDateTime bioUpdatedAt;

    private Long labId;

    /** 是否已正式入组 */
    private Integer placed;

    private LocalDateTime createdAt;
}
