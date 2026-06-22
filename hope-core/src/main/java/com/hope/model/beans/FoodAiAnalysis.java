package com.hope.model.beans;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI分析记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(description = "AI分析记录实体")
@Table(name = "food_ai_analysis")
public class FoodAiAnalysis implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "主键id", name = "id", required = true)
    private Integer id;

    @ApiModelProperty(value = "帖子ID")
    private Integer postId;

    @ApiModelProperty(value = "AI识别的菜品名")
    private String foodName;

    @ApiModelProperty(value = "识别置信度(0-1)")
    private BigDecimal confidence;

    @ApiModelProperty(value = "推测的食材/配料")
    private String ingredients;

    @ApiModelProperty(value = "热量估算(大卡)")
    private Integer caloriesEstimate;

    @ApiModelProperty(value = "AI生成的建议文案")
    private String suggestedCaption;

    @ApiModelProperty(value = "同款美食建议")
    private String similarRecommendation;

    @ApiModelProperty(value = "状态：1待处理 2已完成 3失败")
    private Integer analysisStatus;

    @ApiModelProperty(value = "失败原因")
    private String errorMessage;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }
}
