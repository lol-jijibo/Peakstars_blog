package com.interview.auth.admin.service;

import com.interview.auth.admin.dto.request.AdminBatchUpsertRequest;
import com.interview.auth.admin.dto.request.AdminContentUpsertRequest;
import com.interview.auth.admin.dto.request.AdminDraftUpsertRequest;
import com.interview.auth.admin.dto.response.AdminContentRecordResponse;
import com.interview.auth.admin.dto.response.AdminDashboardResponse;
import com.interview.auth.admin.dto.response.AdminDraftResponse;
import java.util.List;

/**
 * 对外统一提供后台管理台的仪表盘、心跳、内容 CRUD、批量导入和草稿管理能力。
 * Service 层负责不同内容模块的字段适配和实时指标拼装，Controller 只负责组织 HTTP 响应。
 */
public interface AdminService {

    /**
     * 记录后台管理页在线心跳，驱动仪表盘在线人数实时变化。
     * 前端会以固定 clientId 周期上报，后端据此维护短周期在线会话窗口。
     */
    void heartbeat(String clientId);

    /**
     * 聚合后台管理页首页所需的全部统计数据。
     * 统一返回核心指标、趋势点、模块统计和最近编辑，减少管理台首屏请求次数。
     */
    AdminDashboardResponse getDashboard();

    /**
     * 按模块读取后台内容管理列表。
     * 返回的是统一记录结构，前端根据当前模块决定展示和编辑哪些字段。
     */
    List<AdminContentRecordResponse> listContent(String type);

    /**
     * 保存单条后台内容记录，覆盖新增和编辑两种场景。
     * 如果业务主键已存在则执行更新，否则按模块规则生成新主键并插入。
     */
    AdminContentRecordResponse saveContent(String type, String contentKey, AdminContentUpsertRequest request);

    /**
     * 处理 Excel 批量导入后的统一写入动作。
     * 后端逐条执行幂等保存并记录一次批量导入日志，避免前端自行拼接多次请求。
     */
    List<AdminContentRecordResponse> batchSaveContent(String type, AdminBatchUpsertRequest request);

    /**
     * 下线指定内容记录，让管理台删除操作立即反映到前台列表。
     * 采用软删除方式只更新 status，保留历史记录与编辑日志，方便后续恢复。
     */
    void deleteContent(String type, String contentKey);

    // ── 草稿管理 ──────────────────────────────────────────

    List<AdminDraftResponse> listDrafts(String contentType);

    AdminDraftResponse saveDraft(AdminDraftUpsertRequest request);

    void deleteDraft(String draftKey);
}
