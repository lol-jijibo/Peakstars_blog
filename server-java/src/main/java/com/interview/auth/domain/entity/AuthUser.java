package com.interview.auth.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 认证用户实体。
 * 作用：映射 {@code auth_user} 表，承载注册用户的持久化信息。
 */
@Getter
@Setter
public class AuthUser {

    /**
     * 主键 ID。
     * 说明：对应前端 currentUser.id，也是 token 中的用户标识来源。
     */
    private Long id;

    /**
     * 用户名。
     * 作用：支持作为登录账号使用，也用于页面昵称展示。
     */
    private String username;

    /**
     * 邮箱。
     * 作用：支持作为登录账号使用，也是注册时的唯一标识之一。
     */
    private String email;

    /**
     * 密码哈希值。
     * 说明：后端只保存加盐哈希结果，不保存明文密码。
     */
    private String passwordHash;

    /**
     * 用户状态。
     * 说明：1 表示正常可登录，0 可用于后续扩展为禁用状态。
     */
    private Integer status;

    /**
     * 创建时间。
     * 作用：供个人信息展示、审计和后续扩展使用。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     * 作用：用于记录这条用户数据最后一次被修改的时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 获取主键 ID。
     *
     * @return 用户主键 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置主键 ID。
     *
     * @param id 用户主键 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取邮箱。
     *
     * @return 注册邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱。
     *
     * @param email 注册邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取密码哈希值。
     *
     * @return 加盐哈希后的密码串
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置密码哈希值。
     *
     * @param passwordHash 加盐哈希后的密码串
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 获取用户状态。
     *
     * @return 用户状态，1 为正常
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置用户状态。
     *
     * @param status 用户状态
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 获取创建时间。
     *
     * @return 用户创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 用户创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 用户更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 用户更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
