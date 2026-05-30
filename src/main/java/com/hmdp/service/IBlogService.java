package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.dto.admin.AdminBlogReviewDTO;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result queryById(Integer id);

    Result queryHotBlog(Integer current);

    Result likeBlog(Long id);

    Result queryBlogLikes(Integer id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);

    Result queryAdminBlogs(Integer page, Integer size, Integer status, String keyword);

    Result reviewBlog(Long blogId, AdminBlogReviewDTO reviewDTO);

    Result hideBlog(Long id);

    Result restoreBlog(Long id);
}
