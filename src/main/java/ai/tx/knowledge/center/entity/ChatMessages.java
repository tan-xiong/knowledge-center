package ai.tx.knowledge.center.entity;

import cn.hutool.core.lang.UUID;
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


    private Integer tokenCount;

    private LocalDateTime createdAt;

    public void genId() {
        // 后续改为雪花算法
        this.id = UUID.randomUUID().toString(true);
    }

}
