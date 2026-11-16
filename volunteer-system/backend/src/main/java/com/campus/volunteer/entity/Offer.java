package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 拟录取 / 递补 / 内部调剂候选 */
@Data
@TableName("offer")
public class Offer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long mentorId;

    private Long labId;

    private Integer roundNo;

    /** OFFER/WAITLIST/INNER */
    private String type;

    private Integer rankNo;

    /** PENDING/CONFIRMED/DECLINED/EXPIRED/FILLED */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime confirmTime;
}
