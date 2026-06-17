package com.hope.service;

public interface RankingComputeService {

    /**
     * 刷新实时热榜
     * 拉取近6小时帖子 → 按(区域,分类)分组 → 计算热度分 → 取TOP 20 → 写入 food_ranking
     */
    void refreshRealtimeRanking();

    /**
     * 每日金榜结算（0点触发）
     * 拉取当日全部帖子 → 按(区域,分类)分组 → 综合评分 → 取TOP 20 → 锁定写入
     */
    void settleDailyRanking();
}
