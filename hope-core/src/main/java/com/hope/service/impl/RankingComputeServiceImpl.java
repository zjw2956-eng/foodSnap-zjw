package com.hope.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hope.model.beans.FoodPost;
import com.hope.model.beans.FoodRanking;
import com.hope.service.FoodPostService;
import com.hope.service.FoodRankingService;
import com.hope.service.RankingComputeService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RankingComputeServiceImpl implements RankingComputeService {

    private static final int TOP_N = 20;
    private static final int RECENT_HOURS = 6;

    @Autowired
    private FoodPostService foodPostService;

    @Autowired
    private FoodRankingService foodRankingService;

    @Autowired
    private RedissonClient redissonClient;

    // ==================== 实时热榜 ====================

    @Override
    public void refreshRealtimeRanking() {
        RLock lock = redissonClient.getLock("ranking:realtime:lock");
        try {
            if (!lock.tryLock(0, 10, TimeUnit.MINUTES)) {
                log.info("[实时热榜] 获取分布式锁失败，本次跳过");
                return;
            }
            log.info("[实时热榜] 开始刷新");

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, -RECENT_HOURS);
            String since = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            List<FoodPost> posts = foodPostService.listRecentPosts(since);
            if (posts.isEmpty()) {
                log.info("[实时热榜] 近{}小时内无帖子，跳过", RECENT_HOURS);
                return;
            }
            log.info("[实时热榜] 查询到 {} 个帖子", posts.size());

            computeAndSave(posts, today, "realtime", this::calcHotScore);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[实时热榜] 获取锁被中断", e);
        } catch (Exception e) {
            log.error("[实时热榜] 刷新异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ==================== 每日金榜 ====================

    @Override
    public void settleDailyRanking() {
        RLock lock = redissonClient.getLock("ranking:daily:lock");
        try {
            if (!lock.tryLock(0, 10, TimeUnit.MINUTES)) {
                log.info("[每日金榜] 获取分布式锁失败，本次跳过");
                return;
            }
            log.info("[每日金榜] 开始结算");

            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            // 幂等检查：今天是否已结算
            List<FoodRanking> existing = foodRankingService.listRanking(today, "daily", null, null);
            if (!existing.isEmpty()) {
                log.info("[每日金榜] 今日已结算，共 {} 条记录，跳过", existing.size());
                return;
            }

            // 查当日所有正常帖子（today = "2026-06-17"，SQL 用 >= 比较）
            String todayStart = today + " 00:00:00";
            List<FoodPost> posts = foodPostService.listTodayPosts(todayStart);
            if (posts.isEmpty()) {
                log.info("[每日金榜] 今日无帖子，跳过");
                return;
            }
            log.info("[每日金榜] 查询到 {} 个帖子", posts.size());

            computeAndSave(posts, today, "daily", this::calcQualityScore);

            log.info("[每日金榜] 结算完成");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[每日金榜] 获取锁被中断", e);
        } catch (Exception e) {
            log.error("[每日金榜] 结算异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ==================== 通用计算逻辑 ====================

    /**
     * 通用：按(区域,分类)分组 → 排序 → 取TOP N → 写入榜单
     */
    private void computeAndSave(List<FoodPost> posts, String date, String rankingType,
                                java.util.function.Function<FoodPost, BigDecimal> scoreFunc) {
        Map<String, List<FoodPost>> grouped = posts.stream()
                .collect(Collectors.groupingBy(p -> p.getAreaCode() + ":" + p.getCategory()));

        for (Map.Entry<String, List<FoodPost>> entry : grouped.entrySet()) {
            String[] keys = entry.getKey().split(":");
            String areaCode = keys[0];
            String category = keys[1];

            List<FoodPost> sorted = entry.getValue().stream()
                    .sorted(Comparator.comparing(scoreFunc).reversed())
                    .limit(TOP_N)
                    .collect(Collectors.toList());

            List<FoodRanking> rankingList = new ArrayList<>();
            int rank = 1;
            for (FoodPost post : sorted) {
                FoodRanking ranking = new FoodRanking();
                ranking.setPostId(post.getId());
                ranking.setScore(scoreFunc.apply(post));
                ranking.setRankPosition(rank++);
                rankingList.add(ranking);
            }

            foodRankingService.saveRanking(date, rankingType, areaCode, category, rankingList);
        }
    }

    // ==================== 评分公式 ====================

    /**
     * 实时热榜热度分 = 点赞×3 + 评论×2 + 收藏×5
     */
    private BigDecimal calcHotScore(FoodPost post) {
        long score = post.getLikeCount() * 3L
                   + post.getCommentCount() * 2L
                   + post.getCollectCount() * 5L;
        return BigDecimal.valueOf(score);
    }

    /**
     * 每日金榜综合评分 = 点赞×2 + 评论×3 + 收藏×4
     * 权重偏向收藏和评论（代表内容长期价值），点赞权重降低
     */
    private BigDecimal calcQualityScore(FoodPost post) {
        long score = post.getLikeCount() * 2L
                   + post.getCommentCount() * 3L
                   + post.getCollectCount() * 4L;
        return BigDecimal.valueOf(score);
    }
}
