package com.campus.volunteer.controller;

import com.alibaba.excel.EasyExcel;
import com.campus.volunteer.common.R;
import com.campus.volunteer.dto.ObjectionReq;
import com.campus.volunteer.dto.PublicExportRow;
import com.campus.volunteer.service.PublicService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** 公示名单接口 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicService publicService;

    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(publicService.list());
    }

    @PostMapping("/publish")
    public R<Map<String, Object>> publish() {
        return R.ok(publicService.publish());
    }

    @PostMapping("/objection")
    public R<Void> objection(@RequestBody ObjectionReq req) {
        publicService.objection(req.getContent());
        return R.ok();
    }

    /** 导出录取名单 xlsx */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        List<PublicExportRow> rows = publicService.exportRows();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode("录取名单.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ";filename*=UTF-8''" + fileName);
        EasyExcel.write(response.getOutputStream(), PublicExportRow.class)
                .sheet("录取名单")
                .doWrite(rows);
    }
}
