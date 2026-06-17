package com.hope.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hope.service.RedisInteractionService;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis 互动数据同步到 DB 的定时任务
 * 每 5 分钟执行一次，保证 Redis 数据不丢
 */
@Slf4j
@Component
public class SyncInteractionToDbJob implements Job {

    @Autowired
    private RedisInteractionService redisInteractionService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("[定时任务] Redis 互动数据同步到 DB 开始");
        try {
            redisInteractionService.syncAllToDb();
            log.info("[定时任务] Redis 互动数据同步完成");
        } catch (Exception e) {
            log.error("[定时任务] Redis 互动数据同步失败", e);
            throw new JobExecutionException(e);
        }
    }
}
