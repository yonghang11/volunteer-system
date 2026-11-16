package com.campus.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 8 个环节的时间窗 */
@Data
@TableName("stage_window")
public class StageWindow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stageCode;

    private Integer stageNo;

    private String name;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String remark;
}
