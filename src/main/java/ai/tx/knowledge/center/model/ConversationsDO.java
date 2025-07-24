package ai.tx.knowledge.center.model;

import ai.tx.knowledge.center.enums.ConversationsStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * @author tanxiong
 * @date 2025/7/18 15:41
 */
@Data
@Entity
@Table(name = "conversations")
public class ConversationsDO {

    @Id
    private String id;

    private String conversationId;

    private String userId;

    private String title;

    private String category;

    @Enumerated(EnumType.STRING)
    private ConversationsStatus status = ConversationsStatus.ACTIVE;

    private Date lastMessageAt;

    private Date createdAt;

    private Date updatedAt;
}
