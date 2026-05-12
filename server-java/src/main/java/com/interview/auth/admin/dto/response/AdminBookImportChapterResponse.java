package com.interview.auth.admin.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接后台书籍导入审核面板的章节暂存字段。
 * 服务层统一返回章节标题、排序、正文字数和预处理告警，便于后台逐章核验。
 */
@Getter
@Setter
public class AdminBookImportChapterResponse {

    private String tempChapterKey;
    private Integer chapterNo;
    private String title;
    private String subtitle;
    private String sourcePath;
    private String contentHtml;
    private String plainText;
    private Integer wordCount;
    private String reviewStatus;
    private Integer sortOrder;
    private List<String> warnings;
}
