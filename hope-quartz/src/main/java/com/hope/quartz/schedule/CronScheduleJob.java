package com.hope.quartz.schedule;

import com.hope.quartz.job.RefreshRealtimeRankingJob;
import com.hope.quartz.job.SettleDailyRankingJob;
import com.hope.quartz.job.SyncInteractionToDbJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author:aodeng(低调小熊猫)
 * @blog:（http://ilovey.live)
 * @Description: TODO
 * @Date: 19-5-15
 **/
@Component
public class CronScheduleJob {

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    /** 
    * @Description: 启动定时任务
    * @Param: []
    * @return: []
    * @Author: aodeng
    * @Date: 19-5-15
    */ 
    public void scheduleJobsRun() throws SchedulerException{
        Scheduler scheduler=schedulerFactoryBean.getScheduler();
        schedulejobs(scheduler);
    }

    /** 
    * @Description: 构建 jobDetail、CronTrigger
    * @Param: [scheduler]
    * @return: [scheduler]
    * @Author: aodeng
    * @Date: 19-5-15
    */ 
    private void schedulejobs(Scheduler scheduler) throws SchedulerException {
        // 实时热榜刷新 — 每 30 分钟执行一次
        JobDetail realtimeJobDetail = JobBuilder.newJob(RefreshRealtimeRankingJob.class)
                .withIdentity("refreshRealtimeRanking", "ranking").build();
        CronTrigger realtimeTrigger = TriggerBuilder.newTrigger()
                .withIdentity("refreshRealtimeRankingTrigger", "ranking")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 */30 * * * ?"))
                .build();
        scheduler.scheduleJob(realtimeJobDetail, realtimeTrigger);

        // 每日金榜结算 — 每日 0 点执行
        JobDetail dailyJobDetail = JobBuilder.newJob(SettleDailyRankingJob.class)
                .withIdentity("settleDailyRanking", "ranking").build();
        CronTrigger dailyTrigger = TriggerBuilder.newTrigger()
                .withIdentity("settleDailyRankingTrigger", "ranking")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
                .build();
        scheduler.scheduleJob(dailyJobDetail, dailyTrigger);

        // Redis 互动数据同步到 DB — 每 5 分钟执行一次
        JobDetail syncJobDetail = JobBuilder.newJob(SyncInteractionToDbJob.class)
                .withIdentity("syncInteractionToDb", "maintenance").build();
        CronTrigger syncTrigger = TriggerBuilder.newTrigger()
                .withIdentity("syncInteractionToDbTrigger", "maintenance")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 */5 * * * ?"))
                .build();
        scheduler.scheduleJob(syncJobDetail, syncTrigger);
    }
}
