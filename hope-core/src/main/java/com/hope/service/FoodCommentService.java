package com.hope.service;

import java.util.List;

import com.hope.model.beans.FoodComment;
import com.hope.mybatis.service.BaseService;

public interface FoodCommentService extends BaseService<FoodComment> {

    List<FoodComment> listDanmakus(Integer postId, int limit);

    List<FoodComment> listComments(Integer postId, String cursor, int size);
}
