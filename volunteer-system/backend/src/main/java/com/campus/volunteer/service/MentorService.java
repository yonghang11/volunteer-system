package com.campus.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.volunteer.common.BizException;
import com.campus.volunteer.common.Const;
import com.campus.volunteer.common.CurrentUser;
import com.campus.volunteer.entity.Mentor;
import com.campus.volunteer.mapper.MentorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 导师身份解析：MENTOR 账号与 mentor 表通过 user_id 关联。
 */
@Service
@RequiredArgsConstructor
public class MentorService {

    private final MentorMapper mentorMapper;

    /** 当前登录导师对应的导师记录 */
    public Mentor current() {
        CurrentUser.requireRole(Const.Role.MENTOR);
        Mentor mentor = mentorMapper.selectOne(new LambdaQueryWrapper<Mentor>()
                .eq(Mentor::getUserId, CurrentUser.id()));
        if (mentor == null) {
            throw new BizException("当前账号未绑定导师信息，请联系学院管理员");
        }
        return mentor;
    }

    public Mentor byId(Long mentorId) {
        Mentor mentor = mentorMapper.selectById(mentorId);
        if (mentor == null) {
            throw new BizException("导师不存在：" + mentorId);
        }
        return mentor;
    }
}
