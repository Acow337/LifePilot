package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session) throws MessagingException;

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();

    Result queryAdminUsers(Integer page, Integer size, String keyword, Integer status);

    Result updateUserStatus(Long userId, Integer status);

    Result updateUserRole(Long userId, Integer role);

    Result queryAdminUsers(Integer current, String keyword);

    Result banUser(Long userId);

    Result unbanUser(Long userId);
}
