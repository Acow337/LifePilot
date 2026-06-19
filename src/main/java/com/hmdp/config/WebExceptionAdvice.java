package com.hmdp.config;

import com.hmdp.dto.Result;
import com.hmdp.enums.ErrorCode;
import com.hmdp.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    @ExceptionHandler(BizException.class)
    public Result handleBizException(BizException e) {
        return Result.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestParameterException.class})
    public Result handleBadRequestException(Exception e) {
        return Result.fail(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail(ErrorCode.INTERNAL_ERROR, "服务器异常");
    }
}
