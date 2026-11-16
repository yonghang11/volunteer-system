package com.campus.volunteer.common;

import com.campus.volunteer.config.AppProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 自制 token：格式 id|role|exp|signature，signature=HMAC-SHA256(id|role|exp)，有效期读配置。
 */
@Component
@RequiredArgsConstructor
public class TokenUtil {

    private final AppProperties appProperties;

    /** 生成 token，有效期默认 12 小时 */
    public String create(Long userId, String role) {
        long exp = Instant.now().getEpochSecond() + appProperties.getTokenHours() * 3600L;
        String payload = userId + "|" + role + "|" + exp;
        return payload + "|" + DigestUtil.hmacSha256Hex(appProperties.getTokenSecret(), payload);
    }

    /** 校验 token，失败返回 null */
    public Claims verify(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String[] parts = token.split("\\|");
        if (parts.length != 4) {
            return null;
        }
        String payload = parts[0] + "|" + parts[1] + "|" + parts[2];
        String expect = DigestUtil.hmacSha256Hex(appProperties.getTokenSecret(), payload);
        if (!expect.equals(parts[3])) {
            return null;
        }
        try {
            Long userId = Long.valueOf(parts[0]);
            long exp = Long.parseLong(parts[2]);
            if (Instant.now().getEpochSecond() > exp) {
                return null;
            }
            return new Claims(userId, parts[1], exp);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class Claims {
        private final Long userId;
        private final String role;
        private final long exp;
    }
}
