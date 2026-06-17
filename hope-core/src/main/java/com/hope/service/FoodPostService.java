package com.hope.service;

import java.util.List;

import com.hope.model.beans.FoodPost;
import com.hope.mybatis.service.BaseService;

public interface FoodPostService extends BaseService<FoodPost> {

    List<FoodPost> feed(String areaCode, String category, String cursor, int size);

    /**
     * 查询近N小时内的正常帖子，用于实时热榜计算
     */
    List<FoodPost> listRecentPosts(String since);

    /**
     * 查询当日所有正常帖子，用于每日金榜结算
     */
    List<FoodPost> listTodayPosts(String today);

    /**
     * 查询某用户的帖子列表，支持游标分页
     */
    List<FoodPost> listByUserId(Integer userId, String cursor, int size);
}
