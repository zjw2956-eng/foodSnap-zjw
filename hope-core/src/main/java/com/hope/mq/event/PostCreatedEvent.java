package com.hope.mq.event;

import java.io.Serializable;

import lombok.Data;

@Data
public class PostCreatedEvent implements Serializable{
    
    private static final long serialVersionUID = 1L; 

    /**
     * 帖子主键id
     */
    private Integer postId;

    private String postUuid;

    private Integer userId;

    private String imageUrl;

    private Long timestamp;
}
