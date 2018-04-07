package com.thinvent.zhjs.service.report.service.redis.config;

import com.thinvent.zhjs.service.report.config.properties.boot.BootProperties;
import com.thinvent.zhjs.service.report.service.redis.queue.MessagePublisher;
import com.thinvent.zhjs.service.report.service.redis.queue.PubSubStub;
import com.thinvent.zhjs.service.report.service.redis.queue.RedisMessagePublisher;
import com.thinvent.zhjs.service.report.service.redis.queue.RedisMessageSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericToStringSerializer;

import java.util.List;

/**
 * @create by SNOW 2018.04.07
 */
//@Configuration
public class RedisConfiguration {

    @Autowired
    private BootProperties bootProperties;

    @Autowired
    private List<PubSubStub> pubSubStubList;

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(bootProperties.getRedis().getHostname());
        jedisConnectionFactory.setPort(bootProperties.getRedis().getPort());
        jedisConnectionFactory.setPassword(bootProperties.getRedis().getPassword());
        jedisConnectionFactory.setDatabase(bootProperties.getRedis().getDatabase());
        return jedisConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        final RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(jedisConnectionFactory());
        template.setValueSerializer(new GenericToStringSerializer<Object>(Object.class));
        return template;
    }

    @Bean
    PubSubStub pubSubStub1(){
        return new PubSubStub(new ChannelTopic("pubsub:queue1"),new MessageListenerAdapter(new RedisMessageSubscriber()));
    }
    @Bean
    PubSubStub pubSubStub2(){
        return new PubSubStub(new ChannelTopic("pubsub:queue2"),new MessageListenerAdapter(new RedisMessageSubscriber()));
    }

    @Bean
    PubSubStub pubSubStub3(){
        int database = 0;
        String location = "wjw.zhjs.config3";
        //可以利用这个功能进行 实时同步一个值
        return new PubSubStub(new ChannelTopic("__keyspace@"+database+"__:"+location),new MessageListenerAdapter(new RedisMessageSubscriber()));
    }

    @Bean
    RedisMessageListenerContainer redisContainer() {
        final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(jedisConnectionFactory());
        for (PubSubStub pubSubStub:pubSubStubList) {
            container.addMessageListener(pubSubStub.getMessageListener(), pubSubStub.getChannelTopic());
        }
        return container;
    }

    @Bean
    MessagePublisher redisPublisher() {
        return new RedisMessagePublisher(redisTemplate());
    }

}