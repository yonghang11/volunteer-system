package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 实训室 */
@Data
@TableName("lab")
public class Lab {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String direction;

    private String intro;

    private Integer quotaTotal;

    private LocalDateTime createTime;
}
