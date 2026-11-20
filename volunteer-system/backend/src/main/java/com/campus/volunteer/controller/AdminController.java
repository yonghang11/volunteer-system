package com.campus.volunteer.controller;

import com.campus.volunteer.common.R;
import com.campus.volunteer.dto.StageUpdateReq;
import com.campus.volunteer.entity.AuditLog;
import com.campus.volunteer.service.AdminService;
import com.campus.volunteer.service.StageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 学院管理后台接口 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final StageService stageService;
    private final AdminService adminService;

    /** 修改环节时间窗 */
    @PutMapping("/stage/{code}")
    public R<Void> updateStage(@PathVariable String code, @RequestBody StageUpdateReq req) {
        stageService.updateWindow(code, req.getStartTime(), req.getEndTime());
        return R.ok();
    }

    /** 演示用：一键推进到下一环节 */
    @PostMapping("/stage/advance")
    public R<Map<String, Object>> advance() {
        return R.ok(stageService.advance());
    }

    /** 演示用：重置到意向填报阶段（仅学院管理员） */
    @PostMapping("/stage/reset-intent")
    public R<Map<String, Object>> resetIntent() {
        return R.ok(stageService.resetToIntent());
    }

    /** 全站统计概览 */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(adminService.overview());
    }

    /** 最近 50 条审计日志 */
    @GetMapping("/audit")
    public R<List<AuditLog>> audit() {
        return R.ok(adminService.recentAudits());
    }
}
