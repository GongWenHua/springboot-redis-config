package com.thinvent.zhjs.service.report.config.redis;

import lombok.Data;

/**
 * @create by SNOW 2018.04.06
 */
@Data
public class RedisConnectSettings {
    private String host = "127.0.0.1";
    private int port = 6379;
    private String password;
    private int database = 0;
    /**
     * redis 配置中的统一前缀
     */
    private String prefix = "";
}
