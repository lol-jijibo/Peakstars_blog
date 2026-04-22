package com.interview.auth.common;

/**
 * 统一接口响应体。
 * 作用：和前端现有 fetch 处理逻辑对齐，统一返回 {@code code / message / data} 结构。
 *
 * @param <T> 真实业务数据类型
 */
public record ApiResponse<T>(int code, String message, T data) {

    /**
     * 创建默认成功响应。
     *
     * @param data 业务数据
     * @return code 为 0 的成功响应
     * @param <T> 业务数据类型
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "OK", data);
    }

    /**
     * 创建带自定义文案的成功响应。
     *
     * @param message 成功提示文案
     * @param data 业务数据
     * @return code 为 0 的成功响应
     * @param <T> 业务数据类型
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }

    /**
     * 创建失败响应。
     *
     * @param code 业务错误码
     * @param message 失败原因
     * @return data 为空的失败响应
     * @param <T> 预期业务数据类型
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
