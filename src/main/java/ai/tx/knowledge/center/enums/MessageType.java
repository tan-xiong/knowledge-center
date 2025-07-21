package ai.tx.knowledge.center.enums;

import lombok.Getter;

@Getter
public enum MessageType {
        USER,       // 用户消息
        ASSISTANT,  // AI助手消息
        SYSTEM      // 系统消息
    }