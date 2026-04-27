package com.interview.auth.admin.mapper;

import com.interview.auth.admin.entity.ContentEditLog;
import com.interview.auth.domain.entity.AiHotspot;
import com.interview.auth.domain.entity.TechArticle;
import com.interview.auth.domain.entity.WorldNewsIssue;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 业务目的：承接后台管理台的内容查询、编辑写入和仪表盘统计 SQL。
 * 业务逻辑：统一复用 MyBatis 映射管理三类内容，避免在 Service 层拼接原始 SQL。
 */
@Mapper
public interface AdminMapper {

    Integer countPublishedTechArticles();

    Integer countPublishedWorldNewsIssues();

    Integer countPublishedAiHotspots();

    Integer sumTechArticleViews();

    Integer sumWorldNewsReads();

    Integer sumAiHotspotViews();

    Integer sumTechArticleComments();

    Integer sumAiHotspotComments();

    Integer countTodayEdits();

    List<ContentEditLog> findRecentEditLogs(@Param("limit") int limit);

    List<TechArticle> findAllTechArticles();

    List<WorldNewsIssue> findAllWorldNewsIssues();

    List<AiHotspot> findAllAiHotspots();

    int saveTechArticle(TechArticle article);

    int saveWorldNewsIssue(WorldNewsIssue issue);

    int saveAiHotspot(AiHotspot hotspot);

    int disableTechArticle(@Param("articleKey") String articleKey);

    int disableWorldNewsIssue(@Param("issueKey") String issueKey);

    int disableAiHotspot(@Param("hotspotKey") String hotspotKey);

    int insertContentEditLog(ContentEditLog log);
}
