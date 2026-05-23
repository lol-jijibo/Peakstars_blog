package com.interview.auth.admin.mapper;

import com.interview.auth.admin.entity.ContentDraft;
import com.interview.auth.admin.entity.ContentEditLog;
import com.interview.auth.domain.entity.Category;
import com.interview.auth.domain.entity.Interview;
import com.interview.auth.domain.entity.TechArticle;
import com.interview.auth.domain.entity.WorldNewsIssue;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 承接后台管理台的内容查询、编辑写入和仪表盘统计 SQL。
 * 统一复用 MyBatis 映射管理三类内容，避免在 Service 层拼接原始 SQL。
 */
@Mapper
public interface AdminMapper {

    Integer countPublishedTechArticles();

    Integer countPublishedWorldNewsIssues();

    Integer sumTechArticleViews();

    Integer sumWorldNewsReads();

    Integer sumTechArticleComments();

    Integer countTodayEdits();

    List<ContentEditLog> findRecentEditLogs(@Param("limit") int limit);

    List<TechArticle> findAllTechArticles();

    List<WorldNewsIssue> findAllWorldNewsIssues();

    int saveTechArticle(TechArticle article);

    int saveWorldNewsIssue(WorldNewsIssue issue);

    int disableTechArticle(@Param("articleKey") String articleKey);

    int disableWorldNewsIssue(@Param("issueKey") String issueKey);

    Integer countPublishedInterviews();

    Integer sumInterviewViews();

    List<Interview> findAllInterviews();

    Category findCategoryById(@Param("categoryId") Long categoryId);

    Category findCategoryByCode(@Param("categoryCode") String categoryCode);

    int saveInterview(Interview interview);

    int disableInterview(@Param("interviewId") Integer interviewId);

    int insertContentEditLog(ContentEditLog log);

    // ── 草稿管理 ──────────────────────────────────────────
    List<ContentDraft> findDraftsByType(@Param("contentType") String contentType);

    ContentDraft findDraftByKey(@Param("draftKey") String draftKey);

    int upsertDraft(ContentDraft draft);

    int deleteDraft(@Param("draftKey") String draftKey);
}
