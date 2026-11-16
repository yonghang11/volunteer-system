package com.campus.volunteer.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/** 录取名单导出行 */
@Data
public class PublicExportRow {

    @ExcelProperty("序号")
    private Integer index;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String studentName;

    @ExcelProperty("实训室")
    private String labName;

    @ExcelProperty("导师")
    private String mentorName;

    @ExcelProperty("公示时间")
    private String publishedAt;

    @ExcelProperty("状态")
    private String status;
}
