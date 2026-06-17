package com.hope.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hope.service.RankingComputeService;

import lombok.extern.slf4j.Slf4j;

/**
 * 每日金榜结算定时任务
 * 每日 0 点执行
 */
@Slf4j
@Component
public class SettleDailyRankingJob implements Job {

    @Autowired
    private RankingComputeService rankingComputeService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("[定时任务] 每日金榜结算触发");
        try {
            rankingComputeService.settleDailyRanking();
        } catch (Exception e) {
            log.error("[定时任务] 每日金榜结算失败", e);
            throw new JobExecutionException(e);
        }
    }
}
