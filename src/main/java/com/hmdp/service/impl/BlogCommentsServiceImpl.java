package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.dto.blog.BlogCommentCreateDTO;
import com.hmdp.dto.blog.BlogCommentVO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

    @Resource
    private IUserService userService;

    @Resource
    private IBlogService blogService;

    @Override
    @Transactional
    public Result createComment(BlogCommentCreateDTO createDTO) {
        if (createDTO == null || createDTO.getBlogId() == null || StrUtil.isBlank(createDTO.getContent())) {
            return Result.fail("评论参数不完整");
        }
        Blog blog = blogService.getById(createDTO.getBlogId());
        if (blog == null || !Integer.valueOf(SystemConstants.BLOG_STATUS_NORMAL).equals(blog.getStatus())) {
            return Result.fail("笔记不存在或不可评论");
        }

        Long parentId = createDTO.getParentId() == null ? 0L : createDTO.getParentId();
        Long answerId = createDTO.getAnswerId() == null ? 0L : createDTO.getAnswerId();
        if (parentId > 0) {
            BlogComments parent = getById(parentId);
            if (parent == null || !Integer.valueOf(SystemConstants.COMMENT_STATUS_NORMAL).equals(parent.getStatus())) {
                return Result.fail("父评论不存在");
            }
            if (!parent.getBlogId().equals(createDTO.getBlogId())) {
                return Result.fail("评论所属笔记不一致");
            }
        }

        BlogComments comment = new BlogComments();
        comment.setBlogId(createDTO.getBlogId());
        comment.setContent(createDTO.getContent().trim());
        comment.setParentId(parentId);
        comment.setAnswerId(answerId);
        comment.setLiked(0);
        comment.setStatus(SystemConstants.COMMENT_STATUS_NORMAL);
        comment.setUserId(UserHolder.getUser().getId());

        boolean saved = save(comment);
        if (!saved) {
            return Result.fail("评论发布失败");
        }
        blogService.update().setSql("comments = IFNULL(comments,0) + 1").eq("id", createDTO.getBlogId()).update();

        return Result.ok(comment.getId());
    }

    @Override
    public Result queryBlogComments(Long blogId, Integer current) {
        if (blogId == null) {
            return Result.fail("blogId不能为空");
        }
        int pageNo = current == null || current < 1 ? 1 : current;
        Page<BlogComments> topPage = query()
                .eq("blog_id", blogId)
                .eq("parent_id", 0)
                .eq("status", SystemConstants.COMMENT_STATUS_NORMAL)
                .orderByDesc("create_time")
                .page(new Page<>(pageNo, SystemConstants.MAX_PAGE_SIZE));

        List<BlogComments> topComments = topPage.getRecords();
        if (topComments.isEmpty()) {
            return Result.ok(Collections.emptyList(), topPage.getTotal());
        }

        Set<Long> topIds = topComments.stream().map(BlogComments::getId).collect(Collectors.toSet());
        List<BlogComments> replies = query()
                .eq("blog_id", blogId)
                .in("parent_id", topIds)
                .eq("status", SystemConstants.COMMENT_STATUS_NORMAL)
                .orderByAsc("create_time")
                .list();

        List<Long> userIds = new ArrayList<>();
        topComments.forEach(comment -> userIds.add(comment.getUserId()));
        replies.forEach(comment -> userIds.add(comment.getUserId()));

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        }

        Map<Long, BlogCommentVO> topVoMap = new HashMap<>();
        List<BlogCommentVO> result = new ArrayList<>(topComments.size());
        for (BlogComments topComment : topComments) {
            BlogCommentVO vo = toVO(topComment, userMap.get(topComment.getUserId()));
            topVoMap.put(topComment.getId(), vo);
            result.add(vo);
        }

        for (BlogComments reply : replies) {
            BlogCommentVO parentVo = topVoMap.get(reply.getParentId());
            if (parentVo == null) {
                continue;
            }
            parentVo.getReplies().add(toVO(reply, userMap.get(reply.getUserId())));
        }

        return Result.ok(result, topPage.getTotal());
    }

    @Override
    @Transactional
    public Result deleteComment(Long id) {
        if (id == null) {
            return Result.fail("评论id不能为空");
        }
        BlogComments comment = getById(id);
        if (comment == null) {
            return Result.fail("评论不存在");
        }

        UserDTO operator = UserHolder.getUser();
        if (operator == null) {
            return Result.fail("未登录");
        }
        boolean isOwner = operator.getId().equals(comment.getUserId());
        boolean isAdmin = Integer.valueOf(SystemConstants.ROLE_ADMIN).equals(operator.getRole());
        if (!isOwner && !isAdmin) {
            return Result.fail("无权限删除该评论");
        }

        int removed = 0;
        if (Long.valueOf(0L).equals(comment.getParentId())) {
            int children = query().eq("parent_id", id).count();
            boolean removedParent = removeById(id);
            boolean removedChildren = children == 0 || lambdaUpdate().eq(BlogComments::getParentId, id).remove();
            if (!removedParent || !removedChildren) {
                return Result.fail("删除评论失败");
            }
            removed = 1 + children;
        } else {
            if (!removeById(id)) {
                return Result.fail("删除评论失败");
            }
            removed = 1;
        }

        if (removed > 0) {
            final int dec = removed;
            blogService.update().setSql("comments = GREATEST(IFNULL(comments,0) - " + dec + ", 0)")
                    .eq("id", comment.getBlogId())
                    .update();
        }
        return Result.ok();
    }

    @Override
    public boolean hideComment(Long id) {
        return update().set("status", SystemConstants.COMMENT_STATUS_HIDDEN).eq("id", id).update();
    }

    @Override
    public boolean restoreComment(Long id) {
        return update().set("status", SystemConstants.COMMENT_STATUS_NORMAL).eq("id", id).update();
    }

    private BlogCommentVO toVO(BlogComments comment, User user) {
        BlogCommentVO vo = BeanUtil.copyProperties(comment, BlogCommentVO.class);
        if (user != null) {
            vo.setNickName(user.getNickName());
            vo.setIcon(user.getIcon());
        }
        return vo;
    }
}
