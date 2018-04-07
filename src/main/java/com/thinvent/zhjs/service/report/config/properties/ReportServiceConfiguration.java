package com.thinvent.zhjs.service.report.config.properties;

import com.thinvent.zhjs.service.report.config.properties.boot.BootProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @create by SNOW 2018.04.06
 */
@EnableConfigurationProperties({ReportServiceProperties.class,BootProperties.class})
public class ReportServiceConfiguration {
}
