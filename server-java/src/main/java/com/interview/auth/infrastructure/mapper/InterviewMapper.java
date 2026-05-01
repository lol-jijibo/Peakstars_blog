package com.interview.auth.infrastructure.mapper;

import com.interview.auth.domain.entity.Category;
import com.interview.auth.domain.entity.Company;
import com.interview.auth.domain.entity.Interview;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 承接面经模块的全部数据库查询，包括面经列表（支持分类/关键词/分页）、详情、点赞/收藏操作和分类列表。
 * 面经列表通过 JOIN 一次查出公司名称和分类信息；标签使用 GROUP_CONCAT 聚合后在 Service 层拆分，避免 N+1 查询。
 *         点赞和收藏使用原子 UPDATE 语句，保证并发安全。
 */
@Mapper
public interface InterviewMapper {

    /**
     * 按分类、关键词和分页参数查询面经列表数据。
     * 使用 LEFT JOIN 关联公司和分类表，标签用 GROUP_CONCAT 聚合为逗号分隔字符串。
     *         动态 SQL 处理 category 和 keyword 的过滤条件，分页通过 LIMIT/OFFSET 实现。
     *
     * @param category 分类编码，传 null 或 'all' 表示不过滤
     * @param keyword  搜索关键词，对标题/作者/摘要做模糊匹配
     * @param offset   分页偏移量
     * @param pageSize 每页条数
     * @return 面经列表条目（含标签聚合字符串，需 Service 层拆分）
     */
    List<Map<String, Object>> findInterviewList(
        @Param("category") String category,
        @Param("keyword") String keyword,
        @Param("offset") int offset,
        @Param("pageSize") int pageSize
    );

    /**
     * 查询面经列表的总记录数，用于前端分页器计算总页数。
     * 过滤条件与 findInterviewList 保持一致，保证 count 与 list 的数据口径一致。
     *
     * @param category 分类编码
     * @param keyword  搜索关键词
     * @return 匹配条件的总记录数
     */
    long countInterviews(
        @Param("category") String category,
        @Param("keyword") String keyword
    );

    /**
     * 查询一篇面经的完整详情，包含公司描述信息。
     * 使用 LEFT JOIN 一次查出 interview、company、category 三表数据，
     *         标签使用 GROUP_CONCAT 聚合为逗号字符串，在 Service 层做拆分。
     *
     * @param id 面经 ID
     * @return 面经详情数据（含公司描述和标签聚合字符串）
     */
    Map<String, Object> findInterviewDetail(@Param("id") Long id);

    /**
     * 原子自增面经的浏览量，在详情页请求时异步触发。
     * 与点赞/收藏相同使用原子 UPDATE，不阻塞主查询返回。
     *
     * @param id 面经 ID
     * @return 影响行数（0 表示面经不存在）
     */
    int incrementViews(@Param("id") Long id);

    /**
     * 原子自增面经的点赞数，避免并发写入丢失计数。
     * 返回更新影响行数供 Controller 判断面经是否存在。
     *
     * @param id 面经 ID
     * @return 影响行数（0 表示面经不存在）
     */
    int incrementLikes(@Param("id") Long id);

    /**
     * 原子自增面经的收藏数，避免并发写入丢失计数。
     * 返回更新影响行数供 Controller 判断面经是否存在。
     *
     * @param id 面经 ID
     * @return 影响行数（0 表示面经不存在）
     */
    int incrementCollects(@Param("id") Long id);

    /**
     * 查询点赞或收藏后的最新数值，用于前端实时刷新展示。
     * 单独查询避免 UPDATE 返回值不携带字段的问题。
     *
     * @param id 面经 ID
     * @return 面经实体（只取 likes 或 collects 字段）
     */
    Interview findLikesAndCollects(@Param("id") Long id);

    /**
     * 查询所有已发布的面经分类列表，供前端分类导航和筛选下拉使用。
     * 按 sort_order 升序排列，保证分类在前端展示的顺序可控。
     *
     * @return 分类列表
     */
    List<Category> findAllCategories();
}
