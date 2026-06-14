package com.hope.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.hope.model.beans.FoodComment;
import com.hope.mybatis.mapper.BaseMapper;

@Mapper
@Repository
public interface FoodCommentMapper extends BaseMapper<FoodComment> {

    List<FoodComment> selectDanmakusByPostId(@Param("postId") Integer postId, @Param("limit") int limit);

    List<FoodComment> selectCommentsByPostId(@Param("postId") Integer postId,
                                             @Param("cursor") String cursor,
                                             @Param("size") int size);
}
