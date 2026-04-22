package com.interview.auth.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 作用：把认证业务中的异常统一转换成前端可直接消费的响应结构，
 * 避免控制器里到处手写 try/catch。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     * 场景：账号不存在、密码错误、登录凭证失效等可预期错误。
     *
     * @param exception 业务异常对象
     * @return 带业务 code 和提示文案的失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理参数校验异常。
     * 场景：请求体缺字段、邮箱格式不合法、JSON 结构错误等。
     *
     * @param exception 参数校验相关异常
     * @return 统一的参数错误响应
     */
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class
    })
    public ApiResponse<Void> handleValidationException(Exception exception) {
        return ApiResponse.fail(400, "请求参数不合法");
    }

    /**
     * 处理未被识别的系统异常。
     * 场景：数据库异常、空指针、加解密异常等非预期错误。
     *
     * @param exception 未捕获异常
     * @return 通用服务器错误响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.fail(500, "服务器内部错误");
    }
}
