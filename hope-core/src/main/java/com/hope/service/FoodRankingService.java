package com.hope.service;

import java.util.List;

import com.hope.model.beans.FoodRanking;
import com.hope.mybatis.service.BaseService;

public interface FoodRankingService extends BaseService<FoodRanking> {

    /**
     * 查询榜单
     */
    List<FoodRanking> listRanking(String rankingDate, String rankingType,
            String areaCode, String category);

    /**
     * 保存榜单：先删旧数据 + 批量插入新数据（事务）
     *
     * @param rankingDate 榜单日期
     * @param rankingType 榜单类型 realtime/daily
     * @param areaCode    区域编码
     * @param category    分类
     * @param rankingList 排好序的榜单列表（rankPosition 已设置好）
     */
    void saveRanking(String rankingDate, String rankingType,
            String areaCode, String category,
            List<FoodRanking> rankingList);
}
