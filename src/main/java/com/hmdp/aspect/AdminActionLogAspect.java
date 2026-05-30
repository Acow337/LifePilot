package com.hmdp.aspect;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONUtil;
import com.hmdp.annotation.AdminActionLog;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.AdminLog;
import com.hmdp.service.IAdminLogService;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class AdminActionLogAspect {

    @Resource
    private IAdminLogService adminLogService;

    private final ExpressionParser parser = new SpelExpressionParser();

    @AfterReturning(value = "@annotation(adminActionLog)", returning = "ret")
    public void afterReturning(JoinPoint joinPoint, AdminActionLog adminActionLog, Object ret) {
        if (ret instanceof Result && !Boolean.TRUE.equals(((Result) ret).getSuccess())) {
            return;
        }

        UserDTO operator = UserHolder.getUser();
        if (operator == null) {
            return;
        }

        Long targetId = resolveTargetId(joinPoint, adminActionLog.targetId(), ret);
        Map<String, Object> detail = new HashMap<>();
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            detail.put("method", request.getMethod());
            detail.put("uri", request.getRequestURI());
        }

        AdminLog adminLog = new AdminLog()
                .setOperatorId(operator.getId())
                .setModule(adminActionLog.module())
                .setAction(adminActionLog.action())
                .setTargetType(adminActionLog.targetType())
                .setTargetId(targetId == null ? 0L : targetId)
                .setDetail(JSONUtil.toJsonStr(detail))
                .setCreateTime(LocalDateTime.now());
        try {
            adminLogService.save(adminLog);
        } catch (Exception e) {
            // 日志表可能未初始化，不影响主流程
            log.warn("保存管理员操作日志失败: {}", e.getMessage());
        }
    }

    private Long resolveTargetId(JoinPoint joinPoint, String targetIdSpel, Object ret) {
        if (targetIdSpel == null || targetIdSpel.trim().isEmpty()) {
            return null;
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
                if (parameterNames != null && i < parameterNames.length) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            context.setVariable("result", ret);
            Object value = parser.parseExpression(targetIdSpel).getValue(context);
            return Convert.toLong(value);
        } catch (Exception e) {
            log.warn("解析 admin targetId 失败, spel={}, err={}", targetIdSpel, e.getMessage());
            return null;
        }
    }
}
