package com.hope.model.beans;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "food_comment")
public class FoodComment implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String commentUuid;

    private Integer postId;

    private Integer userId;

    private String content;

    private String commentType;

    private Integer danmakuPosition;

    private String danmakuColor;

    private Integer replyTo;

    private Integer status;

    private Date createTime;
}
