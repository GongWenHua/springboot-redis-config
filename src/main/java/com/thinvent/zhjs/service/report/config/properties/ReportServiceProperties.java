package com.thinvent.zhjs.service.report.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @create by SNOW 2018.04.06
 */
@Configuration
@ConfigurationProperties(prefix = "report-service")
@Data
public class ReportServiceProperties {
    private BasicProperties basic = new BasicProperties();
}
