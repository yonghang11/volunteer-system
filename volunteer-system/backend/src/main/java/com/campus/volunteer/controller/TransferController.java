package com.campus.volunteer.controller;

import com.campus.volunteer.common.R;
import com.campus.volunteer.dto.TransferAssignReq;
import com.campus.volunteer.dto.TransferRefuseReq;
import com.campus.volunteer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 统筹调剂接口 */
@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @GetMapping("/pool")
    public R<Map<String, Object>> pool() {
        return R.ok(transferService.pool());
    }

    @PostMapping("/auto")
    public R<Map<String, Object>> auto() {
        return R.ok(transferService.auto());
    }

    @PostMapping("/{id}/assign")
    public R<Void> assign(@PathVariable Long id, @RequestBody TransferAssignReq req) {
        transferService.assign(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/refuse")
    public R<Void> refuse(@PathVariable Long id, @RequestBody TransferRefuseReq req) {
        transferService.refuse(id, req);
        return R.ok();
    }
}
