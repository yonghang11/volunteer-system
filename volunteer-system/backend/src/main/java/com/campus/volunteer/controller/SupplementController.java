package com.campus.volunteer.controller;

import com.campus.volunteer.common.R;
import com.campus.volunteer.dto.SupplementOpenReq;
import com.campus.volunteer.service.SupplementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 分轮补录接口 */
@RestController
@RequestMapping("/api/supplement")
@RequiredArgsConstructor
public class SupplementController {

    private final SupplementService supplementService;

    @GetMapping("/rounds")
    public R<List<Map<String, Object>>> rounds() {
        return R.ok(supplementService.rounds());
    }

    @PostMapping("/open")
    public R<Map<String, Object>> open(@RequestBody SupplementOpenReq req) {
        return R.ok(supplementService.open(req.getRoundNo()));
    }
}
