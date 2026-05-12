package com.interview.auth.domain.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接 star_read 单个榜单卡片的标题、配色与内容列表。
 * 不同榜单只切换描述文案和排序结果，前端可复用同一块卡片结构。
 */
@Getter
@Setter
public class StarReadRankingResponse {

    private String key;
    private String title;
    private String subtitle;
    private String accentColor;
    private List<StarReadBookResponse> books;
}
