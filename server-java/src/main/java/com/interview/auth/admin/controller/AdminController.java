package com.interview.auth.admin.controller;

import com.interview.auth.admin.dto.request.AdminBatchUpsertRequest;
import com.interview.auth.admin.dto.request.AdminContentUpsertRequest;
import com.interview.auth.admin.dto.request.AdminHeartbeatRequest;
import com.interview.auth.admin.dto.response.AdminContentRecordResponse;
import com.interview.auth.admin.dto.response.AdminDashboardResponse;
import com.interview.auth.admin.service.AdminService;
import com.interview.auth.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务目的：对外暴露后台管理页所需的心跳、仪表盘、内容 CRUD 和批量导入接口。
 * 业务逻辑：Controller 只负责接参和包装统一响应结构，具体数据处理交给 AdminService。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 业务目的：记录后台当前访问者的心跳，驱动在线人数实时统计。
     * 业务逻辑：前端会定时发送 clientId，后端据此维护短周期在线会话。
     */
    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(@Valid @RequestBody AdminHeartbeatRequest request) {
        adminService.heartbeat(request.getClientId());
        return ApiResponse.success("Heartbeat updated", null);
    }

    /**
     * 业务目的：获取后台首页仪表盘聚合数据。
     * 业务逻辑：统一返回指标卡片、趋势图、模块统计和最近编辑，减少前端首屏请求数量。
     */
    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.success(adminService.getDashboard());
    }

    /**
     * 业务目的：按模块读取后台内容管理列表。
     * 业务逻辑：type 决定读取技术文章、看天下或 AI 热点，返回结果统一映射成后台记录结构。
     */
    @GetMapping("/content")
    public ApiResponse<List<AdminContentRecordResponse>> listContent(@RequestParam String type) {
        return ApiResponse.success(adminService.listContent(type));
    }

    /**
     * 业务目的：新增指定模块的一条内容记录。
     * 业务逻辑：后端会按模块规则生成业务主键并落库，同时记录一条编辑日志。
     */
    @PostMapping("/content")
    public ApiResponse<AdminContentRecordResponse> createContent(
        @RequestParam String type,
        @Valid @RequestBody AdminContentUpsertRequest request
    ) {
        return ApiResponse.success(adminService.saveContent(type, null, request));
    }

    /**
     * 业务目的：更新指定模块的一条内容记录。
     * 业务逻辑：通过路径上的业务主键定位记录，再按请求体内容执行幂等更新。
     */
    @PutMapping("/content/{contentKey}")
    public ApiResponse<AdminContentRecordResponse> updateContent(
        @RequestParam String type,
        @PathVariable String contentKey,
        @Valid @RequestBody AdminContentUpsertRequest request
    ) {
        return ApiResponse.success(adminService.saveContent(type, contentKey, request));
    }

    /**
     * 业务目的：处理管理台的 Excel 批量导入保存。
     * 业务逻辑：前端先把 XLSX 转成标准记录数组，再统一提交到该接口执行批量写入。
     */
    @PostMapping("/content/batch")
    public ApiResponse<List<AdminContentRecordResponse>> batchSaveContent(
        @RequestParam String type,
        @Valid @RequestBody AdminBatchUpsertRequest request
    ) {
        return ApiResponse.success(adminService.batchSaveContent(type, request));
    }

    /**
     * 业务目的：下线指定模块的一条内容记录。
     * 业务逻辑：删除采用软删除方案，只更新 status 并同步写入编辑日志。
     */
    @DeleteMapping("/content/{contentKey}")
    public ApiResponse<Void> deleteContent(@RequestParam String type, @PathVariable String contentKey) {
        adminService.deleteContent(type, contentKey);
        return ApiResponse.success("Content removed", null);
    }
}
