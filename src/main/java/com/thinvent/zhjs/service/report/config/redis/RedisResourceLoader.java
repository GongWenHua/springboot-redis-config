package com.thinvent.zhjs.service.report.config.redis;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import redis.clients.jedis.Jedis;

/**
 * @create by SNOW 2018.04.06
 */
public class RedisResourceLoader extends DefaultResourceLoader {

    /**
     * 假装是一个协议
     */
    public static final String PROTOCOL = "redis://";

    private RedisFactory redisFactory;

    private Jedis jedis;

    private String prefix = "";

    RedisResourceLoader(RedisConnectSettings redisSettings) {
        redisFactory = new RedisFactory(
                redisSettings.getHost(),
                redisSettings.getPort(),
                redisSettings.getPassword(),
                redisSettings.getDatabase()
        );
        jedis = redisFactory.getConnection();
        prefix = redisSettings.getPrefix();
    }

    @Override
    public Resource getResource(String location) {
        Assert.hasLength(location, "location 不可以为空");
        if (StringUtils.startsWith(location, "redis://")) {
            location = StringUtils.removeStart(location, "redis://");
        }
        if (!"".equals(prefix)) {
            return new RedisResource(jedis, prefix + "." + location);
        }
        return new RedisResource(jedis, location);
    }

}
