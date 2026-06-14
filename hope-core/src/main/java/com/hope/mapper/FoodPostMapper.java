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

}
