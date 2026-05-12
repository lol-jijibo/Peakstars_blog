package com.interview.auth.admin.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 对应数据库 book_source_file 表，封装书籍原始导入文件归档字段。
 * 配合 MyBatis 保存上传包的源文件信息，便于后续追溯与重复导入判断。
 */
@Getter
@Setter
public class BookSourceFile {

    private Long id;
    private String fileKey;
    private String originalName;
    private String storageUrl;
    private Long fileSize;
    private String fileHash;
    private LocalDateTime createdAt;
}
