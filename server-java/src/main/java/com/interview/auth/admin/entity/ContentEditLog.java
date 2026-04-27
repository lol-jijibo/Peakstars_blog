package com.interview.auth.admin.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：记录后台每次内容编辑、批量导入和删除操作，供仪表盘展示最近变更。
 * 业务逻辑：日志只保留操作快照与时间线，避免后台查询最近更新时再反推多张业务表。
 */
@Getter
@Setter
public class ContentEditLog {

    private Long id;
    private String contentType;
    private String contentKey;
    private String actionType;
    private String operatorName;
    private String contentTitle;
    private LocalDateTime createdAt;
}
