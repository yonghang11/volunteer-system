package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 统筹调剂记录 */
@Data
@TableName("transfer_task")
public class TransferTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /** 1 内部调剂 / 2 跨实训室 / 3 约谈安置 */
    private Integer priority;

    private Long fromLabId;

    private Long toLabId;

    private Long toMentorId;

    /** PENDING/ACCEPTED/REFUSED/ASSIGNED */
    private String status;

    private String operator;

    private String talkNote;

    private LocalDateTime createTime;
}
