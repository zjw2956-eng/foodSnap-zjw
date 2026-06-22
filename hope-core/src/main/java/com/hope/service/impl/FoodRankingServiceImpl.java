package com.hope.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hope.mapper.FoodRankingMapper;
import com.hope.model.beans.FoodRanking;
import com.hope.mybatis.service.impl.BaseServiceImpl;
import com.hope.service.FoodRankingService;

@Service
public class FoodRankingServiceImpl extends BaseServiceImpl<FoodRanking> implements FoodRankingService {

    @Autowired
    private FoodRankingMapper foodRankingMapper;

    @Override
    public List<FoodRanking> listRanking(String rankingDate, String rankingType, String areaCode, String category) {
        return foodRankingMapper.selectRanking(rankingDate, rankingType, areaCode, category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRanking(String rankingDate, String rankingType, String areaCode, String category,
            List<FoodRanking> rankingList) {
        // 1.删除旧榜单
        foodRankingMapper.deleteOldRanking(rankingDate, rankingType, areaCode, category);
        //统一设置createTime
        Date now=new Date();
        for(FoodRanking item:rankingList){
            item.setCreateTime(now);
        }
        // 3.批量插入新榜单
        foodRankingMapper.batchInsert(rankingList);
    }

}
