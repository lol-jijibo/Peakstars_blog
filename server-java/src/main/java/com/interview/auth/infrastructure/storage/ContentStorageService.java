package com.interview.auth.infrastructure.storage;

import java.io.InputStream;

/**
 * 统一抽象内容资源上传能力。
 * 隔离后台导入链路与具体对象存储实现，便于后续从 MinIO 平滑扩展到 OSS、COS 或 S3。
 */
public interface ContentStorageService {

    /**
     * 上传导入资源并返回可访问地址。
     * 统一接收流式资源内容与业务路径前缀，确保图片和附件迁移后能直接回填到正文 HTML。
     *
     * @param objectPrefix 业务对象前缀
     * @param fileName 文件名
     * @param inputStream 资源输入流
     * @param size 资源字节大小
     * @param contentType 资源类型
     * @return 迁移后的对外访问地址
     * @throws Exception 上传异常
     */
    String upload(
        String objectPrefix,
        String fileName,
        InputStream inputStream,
        long size,
        String contentType
    ) throws Exception;

    /**
     * 从存储中下载已上传的资源。
     * 用于封面修复、章节重新解析等需要回读原始文件内容的场景。
     *
     * @param resourceUrl upload 方法返回的对外访问地址
     * @return 资源输入流，调用方负责关闭
     * @throws Exception 下载异常（文件不存在或存储不可达）
     */
    InputStream download(String resourceUrl) throws Exception;

    /**
     * 判断当前链接是否已经属于本站资源。
     * 避免重复迁移已落到自有对象存储的图片或附件，降低导入链路的冗余上传成本。
     *
     * @param resourceUrl 资源访问地址
     * @return 是否属于当前对象存储
     */
    boolean isStorageUrl(String resourceUrl);
}
