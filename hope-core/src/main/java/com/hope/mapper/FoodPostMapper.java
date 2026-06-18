package com.hope.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.hope.model.beans.FoodPost;
import com.hope.mybatis.mapper.BaseMapper;

@Mapper
@Repository
public interface FoodPostMapper extends BaseMapper<FoodPost> {

        List<FoodPost> selectFeed(@Param("areaCode") String areaCode,
                        @Param("category") String category,
                        @Param("cursor") String cursor,
                        @Param("size") int size);

        /**
         * 查询近N小时内的正常贴子，用于实时热榜计算
         */
        List<FoodPost> selectRecentPosts(@Param("since") String since);

        /**
         * 查询当日所有正常帖子，用于每日金榜结算
         */
        List<FoodPost> selectTodayPosts(@Param("today") String today);

        /**
         * 查询某用户的所有帖子，按时间倒序
         */
        List<FoodPost> selectByUserId(@Param("userId") Integer userId,
                        @Param("cursor") String cursor,
                        @Param("size") int size);

}
