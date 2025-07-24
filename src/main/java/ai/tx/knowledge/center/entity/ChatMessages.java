package ai.tx.knowledge.center.entity;

import ai.tx.knowledge.center.common.IdUtils;
import lombok.Data;
import org.springframework.ai.chat.messages.MessageType;

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

    private String contentHash;

    private Integer tokenCount;

    private LocalDateTime createdAt;

    /**
     * 生成唯一ID
     */
    public void genId() {
        this.id = IdUtils.generateId();
    }

}
