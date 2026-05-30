package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.dto.blog.BlogCommentCreateDTO;
import com.hmdp.service.IBlogCommentsService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {

    @Resource
    private IBlogCommentsService blogCommentsService;

    @PostMapping
    public Result createComment(@RequestBody BlogCommentCreateDTO createDTO) {
        return blogCommentsService.createComment(createDTO);
    }

    @GetMapping("/of/blog/{blogId}")
    public Result queryCommentsByBlog(@PathVariable("blogId") Long blogId,
                                      @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogCommentsService.queryBlogComments(blogId, current);
    }

    @DeleteMapping("/{id}")
    public Result deleteComment(@PathVariable("id") Long id) {
        return blogCommentsService.deleteComment(id);
    }
}
