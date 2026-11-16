package com.campus.volunteer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.common.R;
import com.campus.volunteer.entity.Lab;
import com.campus.volunteer.entity.SysUser;
import com.campus.volunteer.mapper.LabMapper;
import com.campus.volunteer.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 实训室管理员本人相关接口。
 */
@RestController
@RequestMapping("/api/lab")
@RequiredArgsConstructor
public class LabController {

    private final LabMapper labMapper;
    private final SysUserMapper sysUserMapper;

    /** 当前登录实训室管理员的实训室信息 */
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        Lab lab = currentLab();
        return R.ok(labVo(lab));
    }

    /** 更新当前实训室的基本信息：名称、研究方向、简介、全室名额 */
    @PutMapping("/me")
    public R<Map<String, Object>> updateMe(@RequestBody Map<String, Object> payload) {
        Lab lab = currentLab();

        Object name = payload.get("name");
        if (name != null) {
            String s = String.valueOf(name).trim();
            if (s.isEmpty()) {
                throw new BizException("实训室名称不能为空");
            }
            lab.setName(s);
        }
        Object direction = payload.get("direction");
        if (direction != null) {
            lab.setDirection(String.valueOf(direction).trim());
        }
        Object intro = payload.get("intro");
        if (intro != null) {
            lab.setIntro(String.valueOf(intro));
        }
        Object quotaTotal = payload.get("quotaTotal");
        if (quotaTotal != null) {
            int quota;
            try {
                quota = Integer.parseInt(String.valueOf(quotaTotal));
            } catch (NumberFormatException e) {
                throw new BizException("名额必须是整数");
            }
            if (quota < 0) {
                throw new BizException("名额不能为负数");
            }
            lab.setQuotaTotal(quota);
        }

        labMapper.updateById(lab);
        return R.ok(labVo(lab));
    }

    /** 当前登录实训室管理员绑定的实训室 */
    private Lab currentLab() {
        CurrentUser.requireRole(Const.Role.LAB_ADMIN);
        Long userId = CurrentUser.id();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getLabId() == null) {
            throw new BizException(401, "登录已失效");
        }
        Lab lab = labMapper.selectById(user.getLabId());
        if (lab == null) {
            throw new BizException("未绑定实训室，请联系学院管理员");
        }
        return lab;
    }

    private Map<String, Object> labVo(Lab lab) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", lab.getId());
        vo.put("code", lab.getCode());
        vo.put("name", lab.getName());
        vo.put("direction", lab.getDirection());
        vo.put("intro", lab.getIntro());
        vo.put("quotaTotal", lab.getQuotaTotal());
        vo.put("createTime", lab.getCreateTime());
        return vo;
    }
}
