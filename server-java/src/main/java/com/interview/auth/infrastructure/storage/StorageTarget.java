package com.interview.auth.infrastructure.storage;

/**
 * 定义内容资源按模块路由到哪类对象存储。
 * 统一收敛面经、书籍与默认模块的上传决策，避免不同业务线继续共用同一个存储出口。
 */
public enum StorageTarget {
    MINIO,
    OSS,
    AUTO
}
