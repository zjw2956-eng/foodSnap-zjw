package com.hope.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RocketMQConfig {
    
    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    @Value("${rocketmq.producer.send-message-timeout}")
    private int sendMessageTimeout;

    @Value("${rocketmq.producer.max-message-size}")
    private int maxMessageSize;

    @Value("${rocketmq.producer.retry-times-when-send-failed}")
    private int retryTimes;

    @Bean(initMethod = "start",destroyMethod = "shutdown")
    public DefaultMQProducer defaultMQProducer(){
        DefaultMQProducer producer=new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(sendMessageTimeout);
        producer.setRetryTimesWhenSendFailed(retryTimes);
        producer.setMaxMessageSize(maxMessageSize);
        producer.setVipChannelEnabled(false);
        return producer;
    }
}
