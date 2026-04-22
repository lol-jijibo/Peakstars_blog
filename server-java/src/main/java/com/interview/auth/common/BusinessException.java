package com.interview.auth.common;

/**
 * 业务异常。
 * 作用：用于抛出“邮箱已注册”“密码错误”这类可预期的业务错误，
 * 再交给全局异常处理器统一转换成前端可直接消费的响应格式。
 */
public class BusinessException extends RuntimeException {

    /**
     * 业务错误码。
     * 说明：和前端约定的响应 code 对齐，不直接使用 HTTP 状态码做页面判断。
     */
    private final int code;

    /**
     * 创建业务异常。
     *
     * @param code 业务错误码
     * @param message 展示给前端的错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取业务错误码。
     *
     * @return 当前异常对应的业务 code
     */
    public int getCode() {
        return code;
    }
}
