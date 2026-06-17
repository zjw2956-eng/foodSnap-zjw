package com.hope.service;

import java.util.Map;

/**
 * Redis 互动服务
 * 基于 Redis Set 实现点赞/收藏的原子操作，O(1) 复杂度
 */
public interface RedisInteractionService {

    /**
     * 点赞/取消点赞
     * @return Map: {liked: boolean, count: long}
     */
    Map<String, Object> toggleLike(Integer postId, Integer userId);

    /**
     * 收藏/取消收藏
     * @return Map: {collected: boolean, count: long}
     */
    Map<String, Object> toggleCollect(Integer postId, Integer userId);

    /**
     * 获取点赞数
     */
    long getLikeCount(Integer postId);

    /**
     * 获取收藏数
     */
    long getCollectCount(Integer postId);

    /**
     * 同步帖子点赞数到 DB
     */
    void syncLikeCountToDb(Integer postId);

    /**
     * 同步帖子收藏数到 DB
     */
    void syncCollectCountToDb(Integer postId);

    /**
     * 同步所有帖子互动数据到 DB
     */
    void syncAllToDb();
}
