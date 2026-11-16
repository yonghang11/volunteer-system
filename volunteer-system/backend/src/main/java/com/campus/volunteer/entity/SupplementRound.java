package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 分轮补录轮次 */
@Data
@TableName("supplement_round")
public class SupplementRound {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer roundNo;

    private Integer openFlag;

    /** 本轮剩余名额快照 */
    private String slotsJson;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;
}
