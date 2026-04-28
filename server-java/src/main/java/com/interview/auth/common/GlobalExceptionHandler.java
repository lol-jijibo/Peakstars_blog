package com.interview.auth.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 业务目的：统一接住控制器层抛出的业务异常、参数异常和系统异常，保证前端始终拿到标准响应结构。
 * 业务逻辑：已知业务错误按约定 code 返回，参数错误固定回 400，未知异常统一记录堆栈并输出可排查的错误类型。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务目的：把可预期的业务异常原样透传给前端，避免业务提示被系统错误覆盖。
     * 业务逻辑：直接复用业务异常中的 code 和 message，前端可继续按原有协议展示提示。
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        log.warn("Business exception intercepted: code={}, message={}", exception.getCode(), exception.getMessage());
        return ApiResponse.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 业务目的：统一处理请求体缺字段、字段格式不合法和 JSON 结构错误等输入异常。
     * 业务逻辑：参数异常统一落成 400，同时记录异常类型，便于快速区分是校验失败还是请求结构错误。
     */
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class
    })
    public ApiResponse<Void> handleValidationException(Exception exception) {
        log.warn(
            "Validation exception intercepted: type={}, message={}",
            exception.getClass().getSimpleName(),
            exception.getMessage()
        );
        return ApiResponse.fail(400, "请求参数不合法");
    }

    /**
     * 业务目的：兜底记录未识别系统异常，避免前端只收到无来源的泛化 500。
     * 业务逻辑：完整打印异常堆栈到后端日志，前端仍保持统一的系统错误提示，避免内部实现细节外泄。
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error(
            "Unhandled exception intercepted: type={}, message={}",
            exception.getClass().getName(),
            exception.getMessage(),
            exception
        );
        return ApiResponse.fail(500, "服务器内部错误");
    }
}
