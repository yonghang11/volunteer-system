package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 宣讲 / 开放日排期 */
@Data
@TableName("session_info")
public class SessionInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** BRIEFING/OPEN_DAY */
    private String type;

    private Long labId;

    private String title;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String place;

    private Integer capacity;

    private String content;
}
