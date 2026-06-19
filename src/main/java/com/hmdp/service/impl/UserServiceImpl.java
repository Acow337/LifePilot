package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.enums.ErrorCode;
import com.hmdp.exception.BizException;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.MailUtils;
import com.hmdp.utils.PasswordEncoder;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final BCryptPasswordEncoder BCRYPT_PASSWORD_ENCODER = new BCryptPasswordEncoder();
    //发短信
    @Override
    public Result sendCode(String phone, HttpSession session) throws MessagingException {
        // 1. 判断是否在一级限制条件内
        Boolean oneLevelLimit = stringRedisTemplate.opsForSet().isMember(ONE_LEVERLIMIT_KEY + phone, "1");
        if (oneLevelLimit != null && oneLevelLimit) {
            throw new BizException(ErrorCode.BAD_REQUEST, "您需要等5分钟后再请求");
        }

// 2. 判断是否在二级限制条件内
        Boolean twoLevelLimit = stringRedisTemplate.opsForSet().isMember(TWO_LEVERLIMIT_KEY + phone, "1");
        if (twoLevelLimit != null && twoLevelLimit) {
            throw new BizException(ErrorCode.BAD_REQUEST, "您需要等20分钟后再请求");
        }

// 3. 检查过去1分钟内发送验证码的次数
        long oneMinuteAgo = System.currentTimeMillis() - 60 * 1000;
        long count_oneminute = stringRedisTemplate.opsForZSet().count(SENDCODE_SENDTIME_KEY + phone, oneMinuteAgo, System.currentTimeMillis());
        if (count_oneminute >= 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "距离上次发送时间不足1分钟，请1分钟后重试");
        }

        // 4. 检查发送验证码的次数
        long fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000;
        long count_fiveminute = stringRedisTemplate.opsForZSet().count(SENDCODE_SENDTIME_KEY + phone, fiveMinutesAgo, System.currentTimeMillis());
        if (count_fiveminute % 3 == 2 && count_fiveminute > 5) {
            // 发送了8, 11, 14, ...次，进入二级限制
            stringRedisTemplate.opsForSet().add(TWO_LEVERLIMIT_KEY + phone, "1");
            stringRedisTemplate.expire(TWO_LEVERLIMIT_KEY + phone, 20, TimeUnit.MINUTES);
            throw new BizException(ErrorCode.BAD_REQUEST, "接下来如需再发送，请等20分钟后再请求");
        } else if (count_fiveminute == 5) {
            // 过去5分钟内已经发送了5次，进入一级限制
            stringRedisTemplate.opsForSet().add(ONE_LEVERLIMIT_KEY + phone, "1");
            stringRedisTemplate.expire(ONE_LEVERLIMIT_KEY + phone, 5, TimeUnit.MINUTES);
            throw new BizException(ErrorCode.BAD_REQUEST, "5分钟内已经发送了5次，接下来如需再发送请等待5分钟后重试");
        }

          //生成验证码
        String code = MailUtils.achieveCode();

         //将生成的验证码保持到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        log.info("发送登录验证码：{}", code);
         //发送验证码
        MailUtils.sendtoMail(phone, code);

        // 更新发送时间和次数
        stringRedisTemplate.opsForZSet().add(SENDCODE_SENDTIME_KEY + phone, System.currentTimeMillis() + "", System.currentTimeMillis());

        return Result.ok();
}

    //登录注册
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String password = loginForm.getPassword();
        if (RegexUtils.isEmailInvalid(phone)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "邮箱格式不正确！！");
        }
        if (password == null || password.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }
        User user = query().eq("phone", phone).one();
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        if (Integer.valueOf(SystemConstants.USER_STATUS_BANNED).equals(user.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被封禁");
        }
        if (!verifyPasswordAndAutoUpgrade(user, password)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "密码错误");
        }
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        HashMap<String, String > userMap = new HashMap<>();
        userMap.put("id", String.valueOf(userDTO.getId()));
        userMap.put("nickName", userDTO.getNickName());
        userMap.put("icon", userDTO.getIcon());
        userMap.put("role", String.valueOf(userDTO.getRole() == null ? SystemConstants.ROLE_USER : userDTO.getRole()));
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    private boolean verifyPasswordAndAutoUpgrade(User user, String rawPassword) {
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }

        boolean matches;
        if (isBcryptHash(storedPassword)) {
            matches = BCRYPT_PASSWORD_ENCODER.matches(rawPassword, storedPassword);
        } else {
            matches = rawPassword.equals(storedPassword);
            if (!matches && storedPassword.contains("@")) {
                try {
                    matches = PasswordEncoder.matches(storedPassword, rawPassword);
                } catch (RuntimeException ignored) {
                    matches = false;
                }
            }
            if (matches) {
                String bcryptPassword = BCRYPT_PASSWORD_ENCODER.encode(rawPassword);
                update().set("password", bcryptPassword).eq("id", user.getId()).update();
                user.setPassword(bcryptPassword);
            }
        }
        return matches;
    }

    private boolean isBcryptHash(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    private User createuser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setRole(SystemConstants.ROLE_USER);
        user.setStatus(SystemConstants.USER_STATUS_NORMAL);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX +RandomUtil.randomString(10));
        save(user);
        return user;
    }

    @Override
    public Result sign() {
        //1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        //2. 获取日期
        LocalDateTime now = LocalDateTime.now();
        //3. 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        //4. 获取今天是当月第几天(1~31)
        int dayOfMonth = now.getDayOfMonth();
        //5. 写入Redis  BITSET key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        Long userId = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        int dayOfMonth = now.getDayOfMonth();
        List<Long> result = stringRedisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (result == null || result.isEmpty()) {
            return Result.ok(0);
        }
        int count = 0;
        Long num = result.get(0);
        while (true) {
            if ((num & 1) == 0) {
                break;
            } else {
                count++;
            }
            num = num >>> 1;
        }
        return Result.ok(count);
    }

    @Override
    public Result queryAdminUsers(Integer page, Integer size, String keyword, Integer status) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? SystemConstants.MAX_PAGE_SIZE : Math.min(size, 50);
        Page<User> userPage = query()
                .and(StrUtil.isNotBlank(keyword), q -> q.like("phone", keyword).or().like("nick_name", keyword))
                .eq(status != null, "status", status)
                .orderByDesc("create_time")
                .page(new Page<>(currentPage, pageSize));
        userPage.getRecords().forEach(user -> user.setPassword(null));
        return Result.ok(userPage.getRecords(), userPage.getTotal());
    }

    @Override
    public Result updateUserStatus(Long userId, Integer status) {
        if (userId == null || status == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "参数不能为空");
        }
        if (!Integer.valueOf(SystemConstants.USER_STATUS_NORMAL).equals(status)
                && !Integer.valueOf(SystemConstants.USER_STATUS_BANNED).equals(status)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户状态非法");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (Integer.valueOf(SystemConstants.ROLE_ADMIN).equals(user.getRole())
                && Integer.valueOf(SystemConstants.USER_STATUS_BANNED).equals(status)) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能封禁管理员账号");
        }
        boolean success = update().set("status", status).eq("id", userId).update();
        if (!success) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "更新用户状态失败");
        }
        return Result.ok();
    }

    @Override
    public Result updateUserRole(Long userId, Integer role) {
        if (userId == null || role == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "参数不能为空");
        }
        if (!Integer.valueOf(SystemConstants.ROLE_USER).equals(role)
                && !Integer.valueOf(SystemConstants.ROLE_ADMIN).equals(role)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户角色非法");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        UserDTO operator = UserHolder.getUser();
        if (operator != null
                && operator.getId().equals(userId)
                && Integer.valueOf(SystemConstants.ROLE_USER).equals(role)) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能取消自己的管理员身份");
        }
        boolean success = update().set("role", role).eq("id", userId).update();
        if (!success) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "更新用户角色失败");
        }
        return Result.ok();
    }

    @Override
    public Result queryAdminUsers(Integer current, String keyword) {
        return queryAdminUsers(current, SystemConstants.MAX_PAGE_SIZE, keyword, null);
    }

    @Override
    public Result banUser(Long userId) {
        return updateUserStatus(userId, SystemConstants.USER_STATUS_BANNED);
    }

    @Override
    public Result unbanUser(Long userId) {
        return updateUserStatus(userId, SystemConstants.USER_STATUS_NORMAL);
    }
}
