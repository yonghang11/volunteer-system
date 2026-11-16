package com.campus.volunteer.controller;

import com.campus.volunteer.common.R;
import com.campus.volunteer.service.StageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 环节时间窗接口 */
@RestController
@RequestMapping("/api/stage")
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;

    @GetMapping("/current")
    public R<Map<String, Object>> current() {
        return R.ok(stageService.current());
    }

    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(stageService.list());
    }
}
