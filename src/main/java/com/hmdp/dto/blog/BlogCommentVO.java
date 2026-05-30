package com.hmdp.dto.blog;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class BlogCommentVO {

    private Long id;

    private Long userId;

    private Long blogId;

    private Long parentId;

    private Long answerId;

    private String content;

    private Integer liked;

    private LocalDateTime createTime;

    private String nickName;

    private String icon;

    private List<BlogCommentVO> replies = new ArrayList<>();
}
