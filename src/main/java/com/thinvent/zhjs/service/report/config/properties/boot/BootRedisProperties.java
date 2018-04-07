package com.thinvent.zhjs.service.report.config.properties.boot;

import lombok.Data;

/**
 * @create by SNOW 2018.04.07
 */
@Data
public class BootRedisProperties {
    private String hostname = "127.0.0.1";
    private int port = 6379;
    private String password;
    private int database = 0;
    private boolean sync = false;
    private String prefix = "";
}
