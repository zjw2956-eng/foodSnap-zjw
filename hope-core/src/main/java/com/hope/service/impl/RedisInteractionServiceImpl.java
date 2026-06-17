package com.hope.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hope.model.beans.FoodCollect;
import com.hope.model.beans.FoodLike;
import com.hope.model.beans.FoodPost;
import com.hope.service.FoodCollectService;
import com.hope.service.FoodLikeService;
import com.hope.service.FoodPostService;
import com.hope.service.RedisInteractionService;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis Set 互动服务实现
 *
 * 核心数据结构：
 *   post:like:{postId}    → Redis Set<userId>  点赞用户集合
 *   post:collect:{postId} → Redis Set<userId>  收藏用户集合
 *
 * 设计理念：
 *   - Redis Set 天然去重 + O(1) SADD/SREM/SISMEMBER/SCARD
 *   - 点赞/收藏实时操作 Redis，异步定时同步到 DB（最终一致性）
 *   - 发帖时初始化空 Set，设置 TTL（7天），防止内存泄漏
 */
@Slf4j
@Service
public class RedisInteractionServiceImpl implements RedisInteractionService {

    private static final String LIKE_KEY_PREFIX = "post:like:";
    private static final String COLLECT_KEY_PREFIX = "post:collect:";
    private static final int SET_TTL_DAYS = 7;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private FoodPostService foodPostService;

    @Autowired
    private FoodLikeService foodLikeService;

    @Autowired
    private FoodCollectService foodCollectService;

    @Override
    public Map<String, Object> toggleLike(Integer postId, Integer userId) {
        RSet<Integer> likeSet = redissonClient.getSet(LIKE_KEY_PREFIX + postId);
        boolean liked;

        if (likeSet.contains(userId)) {
            // 已点赞 → 取消
            likeSet.remove(userId);
            liked = false;
        } else {
            // 未点赞 → 点赞
            likeSet.add(userId);
            // 续期 TTL
            likeSet.expire(SET_TTL_DAYS, TimeUnit.DAYS);
            liked = true;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("count", likeSet.size());
        return result;
    }

    @Override
    public Map<String, Object> toggleCollect(Integer postId, Integer userId) {
        RSet<Integer> collectSet = redissonClient.getSet(COLLECT_KEY_PREFIX + postId);
        boolean collected;

        if (collectSet.contains(userId)) {
            collectSet.remove(userId);
            collected = false;
        } else {
            collectSet.add(userId);
            collectSet.expire(SET_TTL_DAYS, TimeUnit.DAYS);
            collected = true;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("collected", collected);
        result.put("count", collectSet.size());
        return result;
    }

    @Override
    public long getLikeCount(Integer postId) {
        RSet<Integer> likeSet = redissonClient.getSet(LIKE_KEY_PREFIX + postId);
        return likeSet.size();
    }

    @Override
    public long getCollectCount(Integer postId) {
        RSet<Integer> collectSet = redissonClient.getSet(COLLECT_KEY_PREFIX + postId);
        return collectSet.size();
    }

    @Override
    public void syncLikeCountToDb(Integer postId) {
        RSet<Integer> likeSet = redissonClient.getSet(LIKE_KEY_PREFIX + postId);
        long redisCount = likeSet.size();

        // 同步 Redis Set 中的用户到 DB
        Set<Integer> userIds = likeSet.readAll();
        for (Integer userId : userIds) {
            FoodLike existing = new FoodLike();
            existing.setPostId(postId);
            existing.setUserId(userId);
            if (foodLikeService.selectOne(existing) == null) {
                FoodLike newLike = new FoodLike();
                newLike.setPostId(postId);
                newLike.setUserId(userId);
                foodLikeService.insert(newLike);
            }
        }

        // 更新帖子计数字段
        FoodPost post = foodPostService.selectById(postId);
        if (post != null) {
            post.setLikeCount((int) redisCount);
            foodPostService.updateSelectiveById(post);
        }
        log.debug("[Redis同步] 帖子 {} 点赞数同步: {}", postId, redisCount);
    }

    @Override
    public void syncCollectCountToDb(Integer postId) {
        RSet<Integer> collectSet = redissonClient.getSet(COLLECT_KEY_PREFIX + postId);
        long redisCount = collectSet.size();

        Set<Integer> userIds = collectSet.readAll();
        for (Integer userId : userIds) {
            FoodCollect existing = new FoodCollect();
            existing.setPostId(postId);
            existing.setUserId(userId);
            if (foodCollectService.selectOne(existing) == null) {
                FoodCollect newCollect = new FoodCollect();
                newCollect.setPostId(postId);
                newCollect.setUserId(userId);
                foodCollectService.insert(newCollect);
            }
        }

        FoodPost post = foodPostService.selectById(postId);
        if (post != null) {
            post.setCollectCount((int) redisCount);
            foodPostService.updateSelectiveById(post);
        }
        log.debug("[Redis同步] 帖子 {} 收藏数同步: {}", postId, redisCount);
    }

    @Override
    public void syncAllToDb() {
        List<FoodPost> allPosts = foodPostService.listAll();
        for (FoodPost post : allPosts) {
            try {
                syncLikeCountToDb(post.getId());
                syncCollectCountToDb(post.getId());
            } catch (Exception e) {
                log.error("[Redis同步] 帖子 {} 同步失败", post.getId(), e);
            }
        }
        log.info("[Redis同步] 全量同步完成，共 {} 个帖子", allPosts.size());
    }
}
