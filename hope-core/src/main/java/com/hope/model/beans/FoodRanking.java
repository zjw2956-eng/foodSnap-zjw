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
 * 美食榜单实体类
 *
 * 每条记录代表某个帖子在特定日期、特定类型榜单中的排名快照。
 * 实时热榜(realtime)会被定时覆盖更新；
 * 每日金榜(daily)在每日0点锁定后不可变。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(description = "美食榜单实体")
@Table(name = "food_ranking")
public class FoodRanking implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "主键ID")
    private Integer id;

    @ApiModelProperty(value = "榜单日期")
    private Date rankingDate;

    @ApiModelProperty(value = "榜单类型：realtime/daily")
    private String rankingType; // realtime / daily

    @ApiModelProperty(value = "帖子ID，关联food_post.id")
    private Integer postId;

    @ApiModelProperty(value = "区域编码")
    private String areaCode;

    @ApiModelProperty(value = "分类：breakfast/lunch/dinner/snack/night_snack/all")
    private String category;

    @ApiModelProperty(value = "综合评分")
    private BigDecimal score;

    @ApiModelProperty(value = "排名位置，从1开始")
    private Integer rankPosition;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;
}
