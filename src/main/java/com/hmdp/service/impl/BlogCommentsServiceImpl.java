package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.BlogComments;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.utils.SystemConstants;
import org.springframework.stereotype.Service;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

    @Override
    public boolean hideComment(Long id) {
        return update().set("status", SystemConstants.COMMENT_STATUS_HIDDEN).eq("id", id).update();
    }

    @Override
    public boolean restoreComment(Long id) {
        return update().set("status", SystemConstants.COMMENT_STATUS_NORMAL).eq("id", id).update();
    }
}
