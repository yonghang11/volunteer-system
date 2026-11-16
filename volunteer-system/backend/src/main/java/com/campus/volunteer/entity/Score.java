package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 面试评分 */
@Data
@TableName("score")
public class Score {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long interviewId;

    private Long studentId;

    private Long mentorId;

    private Long labId;

    private Integer roundNo;

    /** 学习态度 0-25 */
    private Integer attitude;

    /** 逻辑思维 0-25 */
    private Integer logic;

    /** 编程基础 0-25 */
    private Integer coding;

    /** 时间投入 0-25 */
    private Integer commitment;

    private Integer total;

    /** OFFER/WAITLIST/INNER/REJECT */
    private String result;

    private String remark;

    private Long scorer;

    private LocalDateTime scoreTime;
}
