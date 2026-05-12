package com.interview.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 集中管理 star_read 搜索模块的接入参数与开关状态。
 * 被 Spring 容器加载后提供给搜索客户端，统一控制 Elasticsearch 连接信息。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.star-read.search")
public class StarReadSearchProperties {

    private boolean enabled = false;
    private String endpoint = "";
    private String index = "tech_article";
    private String apiKey = "";
    private String username = "";
    private String password = "";
    private int timeoutSeconds = 3;
}
