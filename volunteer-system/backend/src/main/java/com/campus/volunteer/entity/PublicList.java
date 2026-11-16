package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 公示名单 */
@Data
@TableName("public_list")
public class PublicList {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long labId;

    private Long mentorId;

    private LocalDateTime publishedAt;

    private LocalDateTime objectUntil;

    private String objection;

    /** PUBLISHED/OBJECTED/JOINED */
    private String status;
}
