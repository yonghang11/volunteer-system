package com.campus.volunteer.controller;

import com.campus.volunteer.common.R;
import com.campus.volunteer.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 目录接口：实训室、宣讲与开放日 */
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/labs")
    public R<List<Map<String, Object>>> labs() {
        return R.ok(catalogService.labs());
    }

    @GetMapping("/sessions")
    public R<List<Map<String, Object>>> sessions() {
        return R.ok(catalogService.sessions());
    }
}
