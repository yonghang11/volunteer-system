package com.campus.volunteer.dto;

import lombok.Data;

import java.util.List;

/** 意向提交请求（覆盖式） */
@Data
public class IntentSubmitReq {

    /** 轮次，1 为正式填报，2/3 为补录轮 */
    private Integer round;

    /** 平行导师意向，最多 3 个 */
    private List<Long> mentorIds;

    /** 是否服从实训室内部调剂 */
    private Boolean acceptInner;

    /** 是否服从全院跨实训室调剂 */
    private Boolean acceptCross;
}
