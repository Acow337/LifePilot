package com.hmdp.service;

import com.hmdp.entity.BlogComments;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.dto.blog.BlogCommentCreateDTO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogCommentsService extends IService<BlogComments> {

    Result createComment(BlogCommentCreateDTO createDTO);

    Result queryBlogComments(Long blogId, Integer current);

    Result deleteComment(Long id);

    boolean hideComment(Long id);

    boolean restoreComment(Long id);
}
