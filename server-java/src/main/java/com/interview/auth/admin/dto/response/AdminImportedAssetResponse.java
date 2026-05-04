package com.interview.auth.admin.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 承接单个导入资源的迁移结果。
 * 向后台预处理界面反馈图片和附件是否迁移成功，便于运营在正式发布前定位异常资源。
 */
@Getter
@Setter
public class AdminImportedAssetResponse {

    private String assetType;
    private String sourceUrl;
    private String targetUrl;
    private String status;
    private String message;
}
