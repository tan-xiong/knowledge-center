package ai.tx.knowledge.center.enums;

import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
public enum ResultCode {
    
    // 成功
    SUCCESS(200, "操作成功"),
    
    // 客户端错误
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源未找到"),
    
    // 服务器错误
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    
    // 业务错误
    CHAT_ERROR(1001, "聊天服务异常"),
    KNOWLEDGE_UPLOAD_ERROR(1002, "知识文件上传失败"),
    KNOWLEDGE_SEARCH_ERROR(1003, "知识搜索失败"),
    INVALID_CHAT_ID(1004, "无效的聊天ID"),
    EMPTY_MESSAGE(1005, "消息内容不能为空"),
    
    // 会话管理错误
    CONVERSATION_NOT_FOUND(1006, "会话不存在"),
    CONVERSATION_ACCESS_DENIED(1007, "无权访问该会话"),
    CONVERSATION_ALREADY_ARCHIVED(1008, "会话已归档"),
    CONVERSATION_ALREADY_DELETED(1009, "会话已删除"),
    INVALID_CONVERSATION_STATUS(1010, "无效的会话状态"),
    CONVERSATION_OPERATION_FAILED(1011, "会话操作失败");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
} 