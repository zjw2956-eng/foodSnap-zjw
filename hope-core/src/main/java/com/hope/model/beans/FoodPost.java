package com.hope.model.beans;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户上传图片实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(description = "用户上传图片实体")
public class FoodPost implements Serializable{
    private static final long serialVersionUID= 1L;
    
    //主键ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "主键id", name = "id",required = true)
    private Integer id;

    /**
     * 帖子唯一标识
     */
    private String postUuid;

    /**
     * 发帖用户ID，关联 sys_user.id  
     */
    private Integer userId;

    /**
     * 图片URL，用Minio存储
     */
    private String imageUrl;

    /**
     * AI识别菜品名，可控，AI未返回时为空
     */
    private String foodName;

    /**
     * 文案描述，AI生成或用户手写  
     */
    private String description;

    /**
     * 分类, breakfast/lunch/dinner/snack/night_snack
     */
    private String category;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 区域编码，行政区划前6位
     */
    private String areaCode;

    /**
     * 区域名称
     */
    private String areaName;

    /**
     * 点赞数,冗余字段，方便排序
     */
    private Integer likeCount;

    /**
     * 评论数，冗余字段
     */
    private Integer commentCount;
    /**
     * 收藏数，冗余字段
     */
    private Integer collectCount;
    /**
     * 综合评分，榜单排序依据
     */
    private BigDecimal score;
    /**
     * 状态，1正常/2AI分析中/3已下架  
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 修改时间
     */
    private Date updateTime;
    
    public static long getSerialversionuid() {
        return serialVersionUID;
    }
}
