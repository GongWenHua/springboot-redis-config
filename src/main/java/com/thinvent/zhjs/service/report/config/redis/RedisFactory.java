package com.thinvent.zhjs.service.report.config.redis;

import com.thinvent.library.redis.ThinventJedisPoolConfig;
import lombok.Data;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @create by SNOW 2018.04.06
 */
@Data
public class RedisFactory {

    private JedisPool jedisPool;
    private String url;
    private int port;
    private String password;
    private int database;

    public RedisFactory(String url, int port, String password, int database) {
        this.database = database;
        this.password = password;
        this.port = port;
        this.url = url;
        if (jedisPool == null) {
            jedisPool = new JedisPool(url, port);
        }
    }

    public Jedis getConnection() {
        Jedis jedis = jedisPool.getResource();
        jedis.auth(password);
        return jedis;
    }
}
