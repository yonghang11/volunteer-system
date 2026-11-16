package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 面试安排 */
@Data
@TableName("interview")
public class Interview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long labId;

    private Long studentId;

    private Long mentorId;

    private Integer roundNo;

    private LocalDateTime slotStart;

    private String place;

    /** PENDING/DONE/ABSENT */
    private String status;
}
