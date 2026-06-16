package com.hope.controller;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hope.model.beans.FoodRanking;
import com.hope.object.ResponseVo;
import com.hope.service.FoodRankingService;
import com.hope.utils.ResultHopeUtil;

/**
 * 榜单控制器
 */
@RestController
@RequestMapping("/api/ranking")
public class FoodRankingController {

    private static final Set<String> VALID_RANKING_TYPES = new HashSet<>(Arrays.asList("realtime", "daily"));

    private static final Set<String> VALID_CATEGORIES = new HashSet<>(
            Arrays.asList("breakfast", "lunch", "dinner", "snack", "night_snack", "all"));

    @Autowired
    private FoodRankingService foodRankingService;

    /**
     * 查询榜单
     *
     * @param rankingType 榜单类型：realtime(默认) / daily
     * @param areaCode    区域编码（可选，不传查全部区域）
     * @param category    分类（可选，不传默认 all）
     */
    @GetMapping
    public ResponseVo<?> list(@RequestParam(defaultValue = "realtime") String rankingType,
            @RequestParam(required = false) String areaCode,
            @RequestParam(defaultValue = "all") String category) {
        if (!VALID_RANKING_TYPES.contains(rankingType)) {
            return ResultHopeUtil.error("无效的榜单类型，仅支持 realtime / daily");
        }
        if (!VALID_CATEGORIES.contains(category)) {
            return ResultHopeUtil.error("无效的分类");
        }
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        List<FoodRanking> list = foodRankingService.listRanking(today, rankingType, areaCode, category);
        return ResultHopeUtil.success("操作成功", list);
    }
}
