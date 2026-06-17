package com.hope.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hope.service.RankingComputeService;

import lombok.extern.slf4j.Slf4j;

/**
 * 实时热榜刷新定时任务
 * 每 30 分钟执行一次
 */
@Slf4j
@Component
public class RefreshRealtimeRankingJob implements Job {

    @Autowired
    private RankingComputeService rankingComputeService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("[定时任务] 实时热榜刷新触发");
        try {
            rankingComputeService.refreshRealtimeRanking();
        } catch (Exception e) {
            log.error("[定时任务] 实时热榜刷新失败", e);
            throw new JobExecutionException(e);
        }
    }
}
