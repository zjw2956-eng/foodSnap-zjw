package com.hope.mq.consumer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hope.mapper.FoodAiAnalysisMapper;
import com.hope.model.beans.FoodAiAnalysis;
import com.hope.model.beans.FoodPost;
import com.hope.mq.event.PostCreatedEvent;
import com.hope.service.FoodPostService;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AiAnalysisConsumer {

    @Autowired
    private FoodPostService foodPostService;

    @Autowired
    private FoodAiAnalysisMapper foodAiAnalysisMapper;

    private DefaultMQPushConsumer consumer;

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @PostConstruct
    public void start() {
        try {
            consumer = new DefaultMQPushConsumer("ai-analysis-group");
            consumer.setNamesrvAddr(nameServer);
            // 3. 订阅 topic，"*" 表示所有 tag
            consumer.subscribe("post-created", "*");
            // 4. 注册并发监听器（用 MessageListenerConcurrently 类型）
            consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                for (MessageExt msg : msgs) {
                    try {
                        handlePostCreated(msg);
                    } catch (Exception e) {
                        // 任一条失败 → 整批重试
                        log.error("AI分析失败, msgId={}", msg.getMsgId(), e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumer.start();
            log.info("AiAnalysisConsumer 启动完成");
        } catch (Exception e) {
            log.error("AiAnalysisConsumer 启动失败", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }

    private void handlePostCreated(MessageExt msg) {
        // 1.反序列化事件
        String json = new String(msg.getBody(), StandardCharsets.UTF_8);
        PostCreatedEvent event = JSONUtil.toBean(json, PostCreatedEvent.class);
        Integer postId = event.getPostId();
        log.info("收到AI分析任务, postId={}", postId);
        // 2. Mock AI 结果（真实调Python服务留 TODO）
        String mockFoodName = "测试菜品";
        BigDecimal mockConfidence = new BigDecimal("0.90");
        String mockCaption = "这道菜看起来超诱人！";
        String mockIngredients = "主料、配料（知识科普）";
        Integer mockCalories = 500;
        // 3.回写 food_post：foodName、description、status=1
        FoodPost post = foodPostService.selectById(postId);
        if (post == null) {
            log.warn("帖子不存在, postId={}, 跳过", postId);
            return; // 返回 void，外层会 return CONSUME_SUCCESS，不重试
        }
        post.setFoodName(mockFoodName);
        post.setDescription(mockCaption);
        post.setStatus(1);
        foodPostService.updateSelectiveById(post);
        // 4. 回写 food_ai_analysis（status=2 已完成）
        FoodAiAnalysis analysis = new FoodAiAnalysis();
        analysis.setPostId(postId);
        analysis.setFoodName(mockFoodName);
        analysis.setConfidence(mockConfidence);
        analysis.setCaloriesEstimate(mockCalories);
        analysis.setIngredients(mockIngredients);
        analysis.setSuggestedCaption(mockCaption);
        analysis.setAnalysisStatus(2);
        // insertSelective 只插非 null 字段，create_time/update_time 由 DB DEFAULT CURRENT_TIMESTAMP 兜底
        // （strict mode 下 NOT NULL 列不接受 null，insert 会写全字段包括 null 导致失败）
        foodAiAnalysisMapper.insertSelective(analysis);
        log.info("AI分析完成, postId={}", postId);
    }
}
