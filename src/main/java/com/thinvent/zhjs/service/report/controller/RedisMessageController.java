package com.thinvent.zhjs.service.report.controller;

import com.thinvent.zhjs.service.report.config.properties.boot.BootProperties;
import com.thinvent.zhjs.service.report.service.redis.queue.RedisMessagePublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @create by SNOW 2018.04.07
 */
//@RequestMapping("redis/")
//@RestController
public class RedisMessageController {

    @Autowired
    private BootProperties bootProperties;

    @Autowired
    private RedisMessagePublisher messagePublisher;

    /**
     * 发布消息到redis中
     * @param channel
     * @param message
     */
    @PostMapping("publish")
    public void publish(String channel,String message){
        messagePublisher.publish(channel,message);
    }
}
