CREATE TABLE documents
(
    id          VARCHAR(64) PRIMARY KEY COMMENT 'id,与Milvus关联的文档ID',
    document_name  VARCHAR(64) COMMENT '文件名称',
    category    VARCHAR(50) COMMENT '简单分类',
    status      ENUM ('PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'PROCESSING',
    created_at  TIMESTAMP                                  DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP                                  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文档管理表';