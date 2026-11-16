package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 导师（意向填报单位） */
@Data
@TableName("mentor")
public class Mentor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long labId;

    private Long userId;

    private String name;

    private String title;

    private String direction;

    private Integer quota;

    private String intro;

    private Integer acceptCrossTransfer;
}
