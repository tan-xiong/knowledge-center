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
    EMPTY_MESSAGE(1005, "消息内容不能为空");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
} 