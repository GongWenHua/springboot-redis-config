package com.thinvent.zhjs.service.report.config.properties.boot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @create by SNOW 2018.04.07
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "boot.config")
public class BootProperties {
    private BOOT_TYPE from;
    private BootRedisProperties redis = new BootRedisProperties();
}
