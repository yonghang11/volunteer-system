package com.campus.volunteer.controller;

import com.campus.volunteer.common.R;
import com.campus.volunteer.dto.IntentSubmitReq;
import com.campus.volunteer.service.IntentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 意向填报接口 */
@RestController
@RequestMapping("/api/intent")
@RequiredArgsConstructor
public class IntentController {

    private final IntentService intentService;

    @GetMapping("/my")
    public R<Map<String, Object>> my(@RequestParam(required = false) Integer round) {
        return R.ok(intentService.my(round));
    }

    @PostMapping("/submit")
    public R<Map<String, Object>> submit(@RequestBody IntentSubmitReq req) {
        return R.ok(intentService.submit(req));
    }
}
