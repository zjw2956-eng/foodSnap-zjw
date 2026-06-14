package com.hope.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hope.mapper.FoodCommentMapper;
import com.hope.model.beans.FoodComment;
import com.hope.mybatis.service.impl.BaseServiceImpl;
import com.hope.service.FoodCommentService;

@Service
public class FoodCommentServiceImpl extends BaseServiceImpl<FoodComment> implements FoodCommentService {

    @Autowired
    private FoodCommentMapper foodCommentMapper;

    @Override
    public List<FoodComment> listDanmakus(Integer postId, int limit) {
        return foodCommentMapper.selectDanmakusByPostId(postId, limit);
    }

    @Override
    public List<FoodComment> listComments(Integer postId, String cursor, int size) {
        return foodCommentMapper.selectCommentsByPostId(postId, cursor, size);
    }
}
