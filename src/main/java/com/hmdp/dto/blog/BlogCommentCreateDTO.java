package com.hmdp.dto.blog;

import lombok.Data;

@Data
public class BlogCommentCreateDTO {

    private Long blogId;

    private Long parentId;

    private Long answerId;

    private String content;
}
