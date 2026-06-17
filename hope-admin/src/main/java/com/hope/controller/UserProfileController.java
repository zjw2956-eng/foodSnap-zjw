package com.hope.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hope.model.beans.FoodPost;
import com.hope.model.beans.SysUser;
import com.hope.object.ResponseVo;
import com.hope.service.FoodPostService;
import com.hope.service.SysUserService;
import com.hope.utils.ResultHopeUtil;

/**
 * 用户主页控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private FoodPostService foodPostService;

    /**
     * 查看用户主页
     *
     * @param userId 目标用户ID（可选，不传则查看自己）
     */
    @GetMapping("/profile")
    public ResponseVo<?> profile(@RequestParam(required = false) Integer userId) {
        // 未指定用户则查当前登录用户
        if (userId == null) {
            SysUser currentUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
            userId = currentUser.getId();
        }

        SysUser user = sysUserService.selectById(userId);
        if (user == null) {
            return ResultHopeUtil.error("用户不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);

        // 统计数量
        Map<String, Object> stats = new HashMap<>();
        stats.put("postCount", user.getPostCount() != null ? user.getPostCount() : 0);
        stats.put("likedCount", user.getLikedCount() != null ? user.getLikedCount() : 0);
        stats.put("rankingCount", user.getRankingCount() != null ? user.getRankingCount() : 0);
        data.put("stats", stats);

        return ResultHopeUtil.success("操作成功", data);
    }

    /**
     * 查看用户帖子列表（游标分页）
     */
    @GetMapping("/posts")
    public ResponseVo<?> posts(@RequestParam(required = false) Integer userId,
                               @RequestParam(required = false) String cursor,
                               @RequestParam(defaultValue = "10") int size) {
        if (userId == null) {
            SysUser currentUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
            userId = currentUser.getId();
        }

        List<FoodPost> posts = foodPostService.listByUserId(userId, cursor, size);
        return ResultHopeUtil.success("操作成功", posts);
    }
}
