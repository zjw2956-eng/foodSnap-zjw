package com.hope.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.hope.model.beans.FoodRanking;
import com.hope.mybatis.mapper.BaseMapper;


@Mapper
@Repository
public interface FoodRankingMapper extends BaseMapper<FoodRanking> {

        /**
         * 查询榜单：按日期 + 类型 + 区域 + 分类
         * 前端展示榜单时的核心查询
         */
        List<FoodRanking> selectRanking(@Param("rankingDate") String rankingDate,
                        @Param("rankingType") String rankingType,
                        @Param("areaCode") String areaCode,
                        @Param("category") String category);

        /**
         * 删除指定日期+类型+区域+分类的旧榜单记录
         * 实时热榜刷新时先删除旧数据，再批量插入新数据
         */
        int deleteOldRanking(@Param("rankingDate") String rankingDate,
                        @Param("rankingType") String rankingType,
                        @Param("areaCode") String areaCode,
                        @Param("category") String category);

        /**
         * 批量插入榜单记录
         * 一次写入 TOP N 条排名数据
         */
        int batchInsert(@Param("list") List<FoodRanking> list);
}
