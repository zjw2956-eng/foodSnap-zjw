package com.hope.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.hope.model.beans.FoodLike;
import com.hope.mybatis.mapper.BaseMapper;

@Mapper
@Repository
public interface FoodLikeMapper extends BaseMapper<FoodLike> {
}
