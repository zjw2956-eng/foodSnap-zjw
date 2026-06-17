package com.hope.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hope.mapper.FoodPostMapper;
import com.hope.model.beans.FoodPost;
import com.hope.mybatis.service.impl.BaseServiceImpl;
import com.hope.service.FoodPostService;

@Service
public class FoodPostServiceImpl extends BaseServiceImpl<FoodPost> implements FoodPostService {

    @Autowired
    private FoodPostMapper foodPostMapper;

    @Override
    public List<FoodPost> feed(String areaCode, String category, String cursor, int size) {
        return foodPostMapper.selectFeed(areaCode, category, cursor, size);
    }

    @Override
    public List<FoodPost> listRecentPosts(String since) {
        return foodPostMapper.selectRecentPosts(since);
    }

    @Override
    public List<FoodPost> listTodayPosts(String today) {
        return foodPostMapper.selectTodayPosts(today);
    }

    @Override
    public List<FoodPost> listByUserId(Integer userId, String cursor, int size) {
        return foodPostMapper.selectByUserId(userId, cursor, size);
    }
}
