package com.thinvent.zhjs.service.report.service.redis.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @create by SNOW 2018.04.07
 */
//@Service
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    public static List<String> messageList = new ArrayList<String>();

    public void onMessage(final Message message, final byte[] pattern) {
        messageList.add(message.toString());
        log.debug("redis Message received");
        log.debug("channel: " + new String(message.getChannel()));
        log.debug("body   : " + new String(message.getBody()));
    }
}