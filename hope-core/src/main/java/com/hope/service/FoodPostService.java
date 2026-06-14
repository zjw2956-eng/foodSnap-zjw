package com.hope.service;

import java.util.List;

import com.hope.model.beans.FoodPost;
import com.hope.mybatis.service.BaseService;

public interface FoodPostService extends BaseService<FoodPost>{

    List<FoodPost> feed(String areaCode, String category, String cursor, int size);

    
}
