package com.campus.volunteer.dto;

import lombok.Data;

/** 面试评分请求：四项 0-25 整数，总分后端计算 */
@Data
public class InterviewScoreReq {

    private Integer attitude;

    private Integer logic;

    private Integer coding;

    private Integer commitment;

    /** OFFER/WAITLIST/INNER/REJECT */
    private String result;

    private String remark;
}
