package com.campus.volunteer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.common.R;
import com.campus.volunteer.entity.SysUser;
import com.campus.volunteer.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 个人简介文件：上传、查询、删除。仅学生端使用。
 */
@RestController
@RequestMapping("/api/bio")
@RequiredArgsConstructor
public class BioController {

    private final SysUserMapper sysUserMapper;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /** 允许的文件扩展名白名单 */
    private static final String[] ALLOWED_EXT = {"pdf", "doc", "docx", "txt", "png", "jpg", "jpeg"};

    /** 最大单文件大小 15MB */
    private static final long MAX_FILE_SIZE = 15L * 1024 * 1024;

    /** 上传/更新个人简介（旧文件自动覆盖） */
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        CurrentUser.requireRole("STUDENT");
        Long userId = CurrentUser.id();

        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException("文件过大，最大 15MB");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }
        boolean ok = false;
        for (String allowed : ALLOWED_EXT) {
            if (allowed.equals(ext)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new BizException("不支持的文件类型，仅允许：pdf / doc / docx / txt / png / jpg");
        }

        // 创建目录
        File dir = new File(uploadDir, "bio");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成新文件名并保存
        String newName = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
        File dest = new File(dir, newName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }

        // 删除旧文件
        SysUser oldUser = sysUserMapper.selectById(userId);
        if (oldUser != null && oldUser.getBioUrl() != null) {
            try {
                File oldFile = new File(uploadDir, "bio/" + oldUser.getBioUrl().replaceFirst("/uploads/bio/", ""));
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            } catch (Exception ignored) {
                // 删除失败不影响本次上传
            }
        }

        String url = "/uploads/bio/" + newName;
        String savedName = originalName == null ? newName : originalName;
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getBioUrl, url)
                .set(SysUser::getBioName, savedName)
                .set(SysUser::getBioUpdatedAt, LocalDateTime.now()));

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("url", url);
        vo.put("name", savedName);
        vo.put("updatedAt", LocalDateTime.now().toString());
        return R.ok(vo);
    }

    /** 查询当前用户的个人简介信息 */
    @GetMapping("")
    public R<Map<String, Object>> get() {
        CurrentUser.requireRole("STUDENT");
        SysUser user = sysUserMapper.selectById(CurrentUser.id());
        if (user == null) {
            throw new BizException(401, "登录已失效");
        }
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("url", user.getBioUrl());
        vo.put("name", user.getBioName());
        vo.put("updatedAt", user.getBioUpdatedAt());
        vo.put("hasBio", user.getBioUrl() != null);
        return R.ok(vo);
    }

    /** 删除当前用户的个人简介 */
    @DeleteMapping("")
    public R<Void> delete() {
        CurrentUser.requireRole("STUDENT");
        Long userId = CurrentUser.id();
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null && user.getBioUrl() != null) {
            try {
                File oldFile = new File(uploadDir, "bio/" + user.getBioUrl().replaceFirst("/uploads/bio/", ""));
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            } catch (Exception ignored) {
            }
            sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, userId)
                    .set(SysUser::getBioUrl, null)
                    .set(SysUser::getBioName, null)
                    .set(SysUser::getBioUpdatedAt, null));
        }
        return R.ok();
    }
}
