package com.thinvent.zhjs.service.report.config.redis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import redis.clients.jedis.Jedis;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * @create by SNOW 2018.04.06
 */
public class RedisResource implements Resource {

    protected final Log logger = LogFactory.getLog(getClass());

    private Jedis jedis;
    private String location;

    public RedisResource(Jedis jedis, String location) {
        this.jedis = jedis;
        this.location = location;
    }

    @Override
    public boolean exists() {
        boolean isExist = jedis.exists(location);
        logger.debug("[配置] [资源] " + this.location + (isExist ? "存在" : "不存在"));
        return isExist;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public URL getURL() throws IOException {
        throw new IOException();
    }

    @Override
    public URI getURI() throws IOException {
        throw new IOException();
    }

    @Override
    public File getFile() throws IOException {
        return null;
    }

    @Override
    public long contentLength() throws IOException {
        return jedis.bitcount(location);
    }

    @Override
    public long lastModified() throws IOException {
        return 0;
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        return null;
    }

    @Override
    public String getFilename() {
        return location;
    }

    @Override
    public String getDescription() {
        return location;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        logger.debug("[配置] [资源] " + this.location);
        return new ByteArrayInputStream(jedis.get(location).getBytes());
    }
}
