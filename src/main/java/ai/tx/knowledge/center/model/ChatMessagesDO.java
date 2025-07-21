package ai.tx.knowledge.center.model;

import ai.tx.knowledge.center.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author tanxiong
 * @date 2025/7/18 15:52
 */
@Data
@Entity
@Table(name = "chat_messages")
public class ChatMessagesDO {

    @Id
    private String id;

    private String conversationId;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    private String content;


    private Integer tokenCount;

    private LocalDateTime createdAt;
}
