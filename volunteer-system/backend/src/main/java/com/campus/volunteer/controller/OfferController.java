package com.campus.volunteer.controller;

import com.campus.volunteer.common.R;
import com.campus.volunteer.dto.OfferBatchReq;
import com.campus.volunteer.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 拟录取与确认接口 */
@RestController
@RequestMapping("/api/offer")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @PostMapping("/batch")
    public R<Map<String, Object>> batch(@RequestBody OfferBatchReq req) {
        return R.ok(offerService.batch(req));
    }

    @GetMapping("/my")
    public R<Map<String, Object>> my() {
        return R.ok(offerService.my());
    }

    @PostMapping("/{id}/confirm")
    public R<Map<String, Object>> confirm(@PathVariable Long id) {
        return R.ok(offerService.confirm(id));
    }

    @GetMapping("/mentor")
    public R<List<Map<String, Object>>> mentor(@RequestParam(required = false) Integer round) {
        return R.ok(offerService.mentorList(round));
    }
}
