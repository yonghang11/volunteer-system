package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 正式入组记录 */
@Data
@TableName("join_record")
public class JoinRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long labId;

    private Long mentorId;

    private LocalDateTime joinTime;
}
