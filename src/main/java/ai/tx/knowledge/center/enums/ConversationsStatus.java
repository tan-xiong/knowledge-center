package ai.tx.knowledge.center.enums;

import lombok.Getter;

/**
 * @author tanxiong
 * @date 2025/7/18 15:43
 * 会话状态枚举
 */

@Getter
public enum ConversationsStatus {

    ACTIVE("活跃"),
    ARCHIVED("归档"),
    DELETED("删除")
    ;


    private final String message;

    ConversationsStatus(String message) {
        this.message = message;
    }

}
