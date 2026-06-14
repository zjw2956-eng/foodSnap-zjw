package com.hope.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hope.enums.ResponseStatusEnum;
import com.hope.model.beans.FoodPost;
import com.hope.model.beans.SysUser;
import com.hope.object.ResponseVo;
import com.hope.service.FoodPostService;
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

    @PostMapping("/upload")
    public ResponseVo<?> upload(@RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam(value = "description", required = false) String description) throws Exception {
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

    @GetMapping("/feed")
    public ResponseVo<?> feed(@RequestParam(value = "areaCode", required = false) String areaCode,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "10") String size) {
        int limit = Integer.parseInt(size);
        List<FoodPost> list = foodPostService.feed(areaCode, category, cursor, limit);
        return ResultHopeUtil.success("操作成功", list);
    }

}
