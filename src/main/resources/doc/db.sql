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

-- 对话会话表
CREATE TABLE conversations
(
    id          VARCHAR(64) PRIMARY KEY COMMENT '会话ID',
    conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id     VARCHAR(64) COMMENT '用户ID',
    title       VARCHAR(200) COMMENT '对话标题',
    category    VARCHAR(50) COMMENT '知识库分类',
    status      ENUM ('ACTIVE', 'ARCHIVED', 'DELETED') DEFAULT 'ACTIVE' COMMENT '会话状态',
    message_count INT DEFAULT 0 COMMENT '消息数量',
    last_message_at TIMESTAMP NULL COMMENT '最后消息时间',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='对话会话表';

-- 对话消息表
create table chat_messages
(
    id              varchar(64)                          not null comment '消息ID'
        primary key,
    conversation_id varchar(64)                          not null comment '会话ID',
    message_type    enum ('USER', 'ASSISTANT', 'SYSTEM') not null comment '消息类型',
    content         longtext                             not null comment '消息内容',
    content_hash    varchar(128)                         not null comment '消息内容hash(用于去重)',
    metadata        json                                 null comment '元数据(相关文档、tokens等)',
    token_count     int                                  null comment 'Token数量',
    created_at      timestamp default CURRENT_TIMESTAMP  not null comment '创建时间'
)
    comment '对话消息表' charset = utf8mb4;

create index idx_conversation_id
    on chat_messages (conversation_id);

create index idx_created_at
    on chat_messages (created_at);




