package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 学生意向（平行，最多 3 个） */
@Data
@TableName("intent")
public class Intent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long mentorId;

    private Long labId;

    private Integer roundNo;

    /** 服从实训室内部调剂 */
    private Integer acceptInner;

    /** 服从全院跨实训室调剂 */
    private Integer acceptCross;

    /** SUBMITTED/OFFERED/CONFIRMED/RELEASED */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
