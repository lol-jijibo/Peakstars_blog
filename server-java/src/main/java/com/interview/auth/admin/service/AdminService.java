package com.interview.auth.admin.service;

import com.interview.auth.admin.dto.request.AdminBatchUpsertRequest;
import com.interview.auth.admin.dto.request.AdminContentImportPreviewRequest;
import com.interview.auth.admin.dto.request.AdminContentUpsertRequest;
import com.interview.auth.admin.dto.request.AdminDraftUpsertRequest;
import com.interview.auth.admin.dto.response.AdminContentImportPreviewResponse;
import com.interview.auth.admin.dto.response.AdminContentRecordResponse;
import com.interview.auth.admin.dto.response.AdminDashboardResponse;
import com.interview.auth.admin.dto.response.AdminDraftResponse;
import java.util.List;

/**
 * 对外统一提供后台管理台的仪表盘、心跳、内容 CRUD、批量导入和草稿管理能力。
 * Service 层负责不同内容模块的字段适配、导入预处理与实时指标聚合，Controller 只负责组织 HTTP 响应。
 */
public interface AdminService {

    /**
     * 记录后台管理页在线心跳，驱动仪表盘在线人数实时变化。
     * 前端会以固定 clientId 周期上报，后端据此维护短周期在线会话窗口。
     *
     * @param clientId 管理端会话标识
     */
    void heartbeat(String clientId);

    /**
     * 聚合后台管理页首页所需的全部统计数据。
     * 统一返回核心指标、趋势点、模块统计和最近编辑，减少管理台首屏请求次数。
     *
     * @return 后台首页聚合数据
     */
    AdminDashboardResponse getDashboard();

    /**
     * 按模块读取后台内容管理列表。
     * 返回的是统一记录结构，前端根据当前模块决定展示和编辑哪些字段。
     *
     * @param type 内容类型
     * @return 管理台内容列表
     */
    List<AdminContentRecordResponse> listContent(String type);

    /**
     * 保存单条后台内容记录，覆盖新增和编辑两种场景。
     * 如业务主键已存在则执行更新，否则按模块规则生成新主键并写入。
     *
     * @param type 内容类型
     * @param contentKey 路径主键
     * @param request 保存请求
     * @return 保存后的统一记录
     */
    AdminContentRecordResponse saveContent(String type, String contentKey, AdminContentUpsertRequest request);

    /**
     * 处理 Excel 批量导入后的统一写入动作。
     * 后端逐条执行幂等保存并记录一次批量导入日志，避免前端自行拼接多次请求。
     *
     * @param type 内容类型
     * @param request 批量导入请求
     * @return 保存后的统一记录集合
     */
    List<AdminContentRecordResponse> batchSaveContent(String type, AdminBatchUpsertRequest request);

    /**
     * 预处理外部导入内容并返回预览结果。
     * 统一在正式保存前完成 HTML 白名单清洗与资源迁移，保证管理端看到的预览结果就是最终入库结果。
     *
     * @param type 内容类型
     * @param request 导入预处理请求
     * @return 导入预处理结果
     */
    AdminContentImportPreviewResponse previewImportedContent(String type, AdminContentImportPreviewRequest request);

    /**
     * 下线指定内容记录，让管理台删除动作立即反映到前台列表。
     * 采用软删除方式只更新 status，保留历史记录与编辑日志，方便后续恢复。
     *
     * @param type 内容类型
     * @param contentKey 内容主键
     */
    void deleteContent(String type, String contentKey);

    /**
     * 按内容类型读取草稿列表。
     * 统一承接后台待编辑面板，便于运营恢复未发布内容。
     *
     * @param contentType 内容类型
     * @return 草稿列表
     */
    List<AdminDraftResponse> listDrafts(String contentType);

    /**
     * 新增或更新单条后台草稿。
     * 统一承接自动保存和手动保存草稿两类场景。
     *
     * @param request 草稿保存请求
     * @return 保存后的草稿结果
     */
    AdminDraftResponse saveDraft(AdminDraftUpsertRequest request);

    /**
     * 删除指定草稿。
     * 在内容正式发布或运营主动丢弃时清理草稿记录。
     *
     * @param draftKey 草稿主键
     */
    void deleteDraft(String draftKey);

    /**
     * 上传封面图片到对象存储，返回可访问的 URL 地址。
     * 支持主流图片格式（jpg/png/gif/webp/bmp/svg），上传后自动按日期分片存储。
     *
     * @param fileName 原始文件名
     * @param inputStream 文件输入流
     * @param size 文件字节大小
     * @param contentType 文件 MIME 类型
     * @return 上传后的可访问 URL
     * @throws Exception 上传或存储异常
     */
    String uploadCoverImage(String moduleType, String fileName, java.io.InputStream inputStream, long size, String contentType) throws Exception;

    /**
     * 业务目的：给后台富文本正文图片提供独立上传能力，保证文章插图统一沉淀到对象存储。
     * 业务逻辑：复用统一内容存储服务，将正文图片写入富文本目录并返回可直接嵌入 HTML 的地址。
     *
     * @param fileName 原始文件名
     * @param inputStream 文件输入流
     * @param size 文件字节大小
     * @param contentType 文件 MIME 类型
     * @return 上传后的正文图片 URL
     * @throws Exception 上传或存储异常
     */
    String uploadRichTextImage(String moduleType, String fileName, java.io.InputStream inputStream, long size, String contentType) throws Exception;
}
