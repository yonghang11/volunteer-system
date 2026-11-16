package com.campus.volunteer.dto;

import lombok.Data;

/** 环节时间窗改期请求 */
@Data
public class StageUpdateReq {

    private String startTime;

    private String endTime;
}
