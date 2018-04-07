package com.thinvent.zhjs.service.report.service.redis.queue;

import lombok.Data;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;

/**
 * @create by SNOW 2018.04.07
 */
@Data
public class PubSubStub {
    private ChannelTopic channelTopic;
    private MessageListener messageListener;
    public PubSubStub(ChannelTopic channelTopic,MessageListener messageListener){
        this.channelTopic = channelTopic;
        this.messageListener = messageListener;
    }
}
