package com.campus.volunteer.dto;

import lombok.Data;

import java.util.List;

/** 导师批量提交拟录取/递补/内部调剂候选 */
@Data
public class OfferBatchReq {

    private Integer round;

    /** 拟录取学生 */
    private List<Long> offers;

    /** 递补学生（下标即递补顺位） */
    private List<Long> waitlist;

    /** 实训室内部调剂候选 */
    private List<Long> inner;
}
