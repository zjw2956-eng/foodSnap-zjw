package com.hope.mq.producer;

import java.nio.charset.StandardCharsets;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hope.model.beans.FoodPost;
import com.hope.mq.event.PostCreatedEvent;
import com.hope.service.FoodPostService;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PostEventProducer {
    
    @Autowired
    private DefaultMQProducer defaultMQProducer;

    @Autowired
    private FoodPostService foodPostService;

    public void send(Integer postId){
        try {
            FoodPost foodPost=foodPostService.selectById(postId);
            if(foodPost==null){
                return;
            }
            PostCreatedEvent postCreatedEvent=new PostCreatedEvent();
            postCreatedEvent.setPostId(postId);
            postCreatedEvent.setPostUuid(foodPost.getPostUuid());
            postCreatedEvent.setUserId(foodPost.getUserId());
            postCreatedEvent.setImageUrl(foodPost.getImageUrl());
            postCreatedEvent.setTimestamp(System.currentTimeMillis());
            byte[] body=JSONUtil.toJsonStr(postCreatedEvent).getBytes(StandardCharsets.UTF_8);
            Message msg=new Message("post-created", body);
            SendResult sendResult=defaultMQProducer.send(msg);
            log.info("发送成功，msgId:{}", sendResult.getMsgId());
        } catch (Exception e) {
            log.error("发送帖子事件失败, postId={}", postId, e);
        } 
    }
}