package com.campus.volunteer.controller;

import com.campus.volunteer.common.R;
import com.campus.volunteer.dto.InterviewScoreReq;
import com.campus.volunteer.service.InterviewService;
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

/** 面试安排与评分接口 */
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/my")
    public R<List<Map<String, Object>>> my() {
        return R.ok(interviewService.my());
    }

    @PostMapping("/generate")
    public R<Map<String, Object>> generate(@RequestBody(required = false) Map<String, Integer> body) {
        Integer round = body == null ? null : body.get("round");
        return R.ok(interviewService.generate(round));
    }

    @GetMapping("/lab")
    public R<List<Map<String, Object>>> lab(@RequestParam(required = false) Integer round) {
        return R.ok(interviewService.labView(round));
    }

    @PostMapping("/{id}/score")
    public R<Map<String, Object>> score(@PathVariable Long id, @RequestBody InterviewScoreReq req) {
        return R.ok(interviewService.score(id, req));
    }
}
