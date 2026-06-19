package com.hmdp.dto;

import com.hmdp.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Boolean success;
    private String errorCode;
    private String errorMsg;
    private Object data;
    private Long total;

    public static Result ok(){
        return new Result(true, ErrorCode.OK.code(), null, null, null);
    }
    public static Result ok(Object data){
        return new Result(true, ErrorCode.OK.code(), null, data, null);
    }
    public static Result ok(List<?> data, Long total){
        return new Result(true, ErrorCode.OK.code(), null, data, total);
    }
    public static Result fail(String errorMsg){
        return new Result(false, ErrorCode.BIZ_ERROR.code(), errorMsg, null, null);
    }
    public static Result fail(ErrorCode errorCode, String errorMsg){
        return new Result(false, errorCode.code(), errorMsg, null, null);
    }
}
