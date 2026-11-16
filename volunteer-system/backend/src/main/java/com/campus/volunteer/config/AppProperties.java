package com.campus.volunteer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置（对应 application.yml 的 app 节点）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** token 签名密钥 */
    private String tokenSecret = "volunteer-system-2026";

    /** token 有效期（小时） */
    private Integer tokenHours = 12;
}
