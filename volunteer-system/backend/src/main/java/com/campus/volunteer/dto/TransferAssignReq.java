package com.campus.volunteer.dto;

import lombok.Data;

/** 调剂指派请求 */
@Data
public class TransferAssignReq {

    private Long mentorId;

    private String note;
}
