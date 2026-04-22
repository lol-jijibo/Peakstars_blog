package com.interview.auth.infrastructure.mapper;

import com.interview.auth.domain.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户认证 Mapper。
 * 作用：把认证模块对 {@code auth_user} 表的读写统一收口到数据访问层。
 */
@Mapper
public interface AuthUserMapper {

    /**
     * 按邮箱查询用户。
     *
     * @param email 标准化后的邮箱地址
     * @return 命中的用户；不存在时返回 null
     */
    AuthUser findByEmail(@Param("email") String email);

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名
     * @return 命中的用户；不存在时返回 null
     */
    AuthUser findByUsername(@Param("username") String username);

    /**
     * 按主键查询用户。
     *
     * @param id 用户主键 ID
     * @return 命中的用户；不存在时返回 null
     */
    AuthUser findById(@Param("id") Long id);

    /**
     * 按邮箱或用户名查询用户。
     * 作用：兼容前端“邮箱 / 用户名二选一”的登录方式。
     *
     * @param account 登录账号，可能是邮箱，也可能是用户名
     * @return 命中的用户；不存在时返回 null
     */
    AuthUser findByEmailOrUsername(@Param("account") String account);

    /**
     * 插入新用户。
     *
     * @param authUser 待落库的认证用户实体
     * @return 影响行数
     */
    int insert(AuthUser authUser);
}
