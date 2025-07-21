package ai.tx.knowledge.center.entity;

import ai.tx.knowledge.center.enums.MessageType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author tanxiong
 * @date 2025/7/18 15:52
 */
@Data
public class ChatMessages implements Serializable {

    private String id;

    private String conversationId;

    private MessageType messageType;

    private String content;


    private Integer tokenCount;

    private LocalDateTime createdAt;
}
