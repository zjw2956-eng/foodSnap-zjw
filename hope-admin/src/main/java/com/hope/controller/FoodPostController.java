package com.hope.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hope.enums.ResponseStatusEnum;
import com.hope.model.beans.FoodCollect;
import com.hope.model.beans.FoodComment;
import com.hope.model.beans.FoodLike;
import com.hope.model.beans.FoodPost;
import com.hope.model.beans.FoodSensitiveWord;
import com.hope.model.beans.SysUser;
import com.hope.object.ResponseVo;
import com.hope.service.FoodCollectService;
import com.hope.service.FoodCommentService;
import com.hope.service.FoodLikeService;
import com.hope.service.FoodPostService;
import com.hope.service.FoodSensitiveWordService;
import com.hope.utils.MinioUtil;
import com.hope.utils.ResultHopeUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 帖子上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/food-post")
public class FoodPostController {

    @Autowired
    private FoodPostService foodPostService;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private FoodLikeService foodLikeService;

    @Autowired
    private FoodCollectService foodCollectService;

    @Autowired
    private FoodCommentService foodCommentService;

    @Autowired
    private FoodSensitiveWordService foodSensitiveWordService;

    /**
     * 用户发帖
     */
    @PostMapping("/upload")
    public ResponseVo<?> upload(@RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "latitude", required = false) BigDecimal latitude,
            @RequestParam(value = "longitude", required = false) BigDecimal longitude) throws Exception {
        // 1. 校验文件（≤10MB，格式 jpg/png/webp）
        if (file.getSize() > 10 * 1024 * 1024L) {
            return ResultHopeUtil.error("文件大小超过10M");
        }
        Set<String> allowedTypes = new HashSet<>(Arrays.asList("image/jpeg", "image/png", "image/webp"));
        if (!allowedTypes.contains(file.getContentType())) {
            return ResultHopeUtil.error("仅支持 jpg/png/webp 格式");
        }
        if (file.getOriginalFilename() == null) {
            return ResultHopeUtil.error("文件名为空");
        }
        String extendName = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        // 2. 生成文件名 = UUID + 扩展名(.jpg/.png...)
        String objectName = UUID.randomUUID().toString() + extendName;
        // 3. minioUtil.upload(file.getInputStream(), 文件名, file.getContentType(),
        // file.getSize()) → URL
        String url = minioUtil.upload(file.getInputStream(), objectName, file.getContentType(), file.getSize());
        // 4. new FoodPost() → set imageUrl、category、description、status=1
        FoodPost foodPost = new FoodPost();
        foodPost.setPostUuid(UUID.randomUUID().toString().replace("-", ""));
        SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
        foodPost.setUserId(user.getId());
        foodPost.setImageUrl(url);
        foodPost.setCategory(category);
        if (description != null && !description.isEmpty()) {
            foodPost.setDescription(description);
        }
        if (latitude != null) {
            foodPost.setLatitude(latitude);
        }
        if (longitude != null) {
            foodPost.setLongitude(longitude);
        }
        foodPost.setStatus(1);
        // 5. foodPostService.insert(foodPost)
        foodPostService.insert(foodPost);
        // 6. return new ResponseVo(ResponseStatusEnum.SUCCESS, foodPost)
        return new ResponseVo(ResponseStatusEnum.SUCCESS, foodPost);
    }

    @GetMapping("/{postUuid}")
    public ResponseVo<?> getPostDetail(@PathVariable String postUuid) {
        FoodPost foodPost = new FoodPost();
        foodPost.setPostUuid(postUuid);
        FoodPost result = foodPostService.selectOne(foodPost);
        if (result == null) {
            return ResultHopeUtil.error("帖子不存在");
        }
        return ResultHopeUtil.success("操作成功", result);
    }

    /**
     * feed流查询
     */
    @GetMapping("/feed")
    public ResponseVo<?> feed(@RequestParam(value = "areaCode", required = false) String areaCode,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "10") String size) {
        int limit = Integer.parseInt(size);
        List<FoodPost> list = foodPostService.feed(areaCode, category, cursor, limit);
        return ResultHopeUtil.success("操作成功", list);
    }

    /**
     * 删帖
     */
    @PutMapping("/{postUuid}/offline")
    public ResponseVo<?> deletePost(@PathVariable String postUuid) {
        FoodPost foodPost = new FoodPost();
        foodPost.setPostUuid(postUuid);
        FoodPost result = foodPostService.selectOne(foodPost);
        if (result == null) {
            return ResultHopeUtil.error("帖子不存在");
        }
        SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
        Integer currentUserId = user.getId();
        // 对比帖子作者
        if (!result.getUserId().equals(currentUserId)) {
            return ResultHopeUtil.error("无权操作");
        }
        result.setStatus(3);
        foodPostService.updateSelectiveById(result);
        return ResultHopeUtil.success("删除成功");
    }

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/{postUuid}/like")
    public ResponseVo<?> toggleLike(@PathVariable String postUuid) {
        SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();

        FoodPost query = new FoodPost();
        query.setPostUuid(postUuid);
        FoodPost post = foodPostService.selectOne(query);
        if (post == null) {
            return ResultHopeUtil.error("帖子不存在");
        }

        // 查询是否已点赞
        FoodLike likeQuery = new FoodLike();
        likeQuery.setPostId(post.getId());
        likeQuery.setUserId(user.getId());
        FoodLike existing = foodLikeService.selectOne(likeQuery);

        if (existing != null) {
            // 已点赞 → 取消
            foodLikeService.deleteById(existing.getId());
        } else {
            // 未点赞 → 点赞
            FoodLike newLike = new FoodLike();
            newLike.setPostId(post.getId());
            newLike.setUserId(user.getId());
            foodLikeService.insert(newLike);
        }

        // 更新帖子点赞数
        FoodLike countQuery = new FoodLike();
        countQuery.setPostId(post.getId());
        int newCount = foodLikeService.select(countQuery).size();
        post.setLikeCount(newCount);
        foodPostService.updateSelectiveById(post);

        Map<String, Object> data = new HashMap<>();
        data.put("liked", existing == null);
        data.put("likeCount", newCount);
        return ResultHopeUtil.success(existing != null ? "取消点赞" : "点赞成功", data);
    }

    /**
     * 收藏/取消收藏
     */
    @PostMapping("/{postUuid}/collect")
    public ResponseVo<?> toggleCollect(@PathVariable String postUuid) {
        SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();

        FoodPost query = new FoodPost();
        query.setPostUuid(postUuid);
        FoodPost post = foodPostService.selectOne(query);
        if (post == null) {
            return ResultHopeUtil.error("帖子不存在");
        }

        // 查询是否已收藏
        FoodCollect collectQuery = new FoodCollect();
        collectQuery.setPostId(post.getId());
        collectQuery.setUserId(user.getId());
        FoodCollect existing = foodCollectService.selectOne(collectQuery);

        if (existing != null) {
            // 已收藏 → 取消
            foodCollectService.deleteById(existing.getId());
        } else {
            // 未收藏 → 收藏
            FoodCollect newCollect = new FoodCollect();
            newCollect.setPostId(post.getId());
            newCollect.setUserId(user.getId());
            foodCollectService.insert(newCollect);
        }

        // 更新帖子收藏数
        FoodCollect countQuery = new FoodCollect();
        countQuery.setPostId(post.getId());
        int newCount = foodCollectService.select(countQuery).size();
        post.setCollectCount(newCount);
        foodPostService.updateSelectiveById(post);

        Map<String, Object> data = new HashMap<>();
        data.put("collected", existing == null);
        data.put("collectCount", newCount);
        return ResultHopeUtil.success(existing != null ? "取消收藏" : "收藏成功", data);
    }

    /**
     * 发弹幕
     */
    @PostMapping("/{postUuid}/danmaku")
    public ResponseVo<?> sendDanmaku(@PathVariable String postUuid,
            @RequestParam("content") String content,
            @RequestParam(value = "danmakuPosition", required = false) Integer danmakuPosition,
            @RequestParam(value = "danmakuColor", defaultValue = "#ffffff") String danmakuColor) {
        // 敏感词过滤
        String error = checkSensitiveWord(content);
        if (error != null) {
            return ResultHopeUtil.error(error);
        }

        SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();

        FoodPost query = new FoodPost();
        query.setPostUuid(postUuid);
        FoodPost post = foodPostService.selectOne(query);
        if (post == null) {
            return ResultHopeUtil.error("帖子不存在");
        }

        FoodComment comment = new FoodComment();
        comment.setCommentUuid(UUID.randomUUID().toString().replace("-", ""));
        comment.setPostId(post.getId());
        comment.setUserId(user.getId());
        comment.setContent(content);
        comment.setCommentType("danmaku");
        comment.setDanmakuPosition(danmakuPosition);
        comment.setDanmakuColor(danmakuColor);
        comment.setStatus(1);
        foodCommentService.insert(comment);

        updateCommentCount(post);

        return ResultHopeUtil.success("弹幕发送成功", comment);
    }

    /**
     * 获取弹幕列表
     */
    @GetMapping("/{postUuid}/danmakus")
    public ResponseVo<?> listDanmakus(@PathVariable String postUuid,
            @RequestParam(value = "count", defaultValue = "50") String count) {
        FoodPost query = new FoodPost();
        query.setPostUuid(postUuid);
        FoodPost post = foodPostService.selectOne(query);
        if (post == null) {
            return ResultHopeUtil.error("帖子不存在");
        }

        int limit = Integer.parseInt(count);
        List<FoodComment> list = foodCommentService.listDanmakus(post.getId(), limit);
        return ResultHopeUtil.success("操作成功", list);
    }

    /**
     * 发评论
     */
    @PostMapping("/{postUuid}/comment")
    public ResponseVo<?> sendComment(@PathVariable String postUuid,
            @RequestParam("content") String content,
            @RequestParam(value = "replyTo", required = false) Integer replyTo) {
        // 敏感词过滤
        String error = checkSensitiveWord(content);
        if (error != null) {
            return ResultHopeUtil.error(error);
        }

        SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();

        FoodPost query = new FoodPost();
        query.setPostUuid(postUuid);
        FoodPost post = foodPostService.selectOne(query);
        if (post == null) {
            return ResultHopeUtil.error("帖子不存在");
        }

        // 如果回复某条评论，检查目标评论是否存在
        if (replyTo != null) {
            FoodComment target = foodCommentService.selectById(replyTo);
            if (target == null || target.getStatus() != 1) {
                return ResultHopeUtil.error("回复的评论不存在");
            }
        }

        FoodComment comment = new FoodComment();
        comment.setCommentUuid(UUID.randomUUID().toString().replace("-", ""));
        comment.setPostId(post.getId());
        comment.setUserId(user.getId());
        comment.setContent(content);
        comment.setCommentType("normal");
        comment.setReplyTo(replyTo);
        comment.setStatus(1);
        foodCommentService.insert(comment);

        updateCommentCount(post);

        return ResultHopeUtil.success("评论发送成功", comment);
    }

    /**
     * 获取评论列表
     */
    @GetMapping("/{postUuid}/comments")
    public ResponseVo<?> listComments(@PathVariable String postUuid,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "10") String size) {
        FoodPost query = new FoodPost();
        query.setPostUuid(postUuid);
        FoodPost post = foodPostService.selectOne(query);
        if (post == null) {
            return ResultHopeUtil.error("帖子不存在");
        }

        int limit = Integer.parseInt(size);
        List<FoodComment> list = foodCommentService.listComments(post.getId(), cursor, limit);
        return ResultHopeUtil.success("操作成功", list);
    }

    /**
     * 敏感词过滤，返回 null 表示通过，返回非 null 为错误信息
     */
    private String checkSensitiveWord(String content) {
        List<FoodSensitiveWord> words = foodSensitiveWordService.listAll();
        for (FoodSensitiveWord w : words) {
            if (content.contains(w.getWord())) {
                return "内容包含敏感词，请修改后重试";
            }
        }
        return null;
    }

    /**
     * 更新帖子评论数
     */
    private void updateCommentCount(FoodPost post) {
        FoodComment countQuery = new FoodComment();
        countQuery.setPostId(post.getId());
        countQuery.setStatus(1);
        int newCount = foodCommentService.select(countQuery).size();
        post.setCommentCount(newCount);
        foodPostService.updateSelectiveById(post);
    }
}
