package com.thinvent.zhjs.service.report.service.redis.queue;

/**
 * @create by SNOW 2018.04.07
 */
public interface MessagePublisher {
    void publish(String topic, final String message);
}
