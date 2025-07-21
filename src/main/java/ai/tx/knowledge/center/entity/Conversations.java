package ai.tx.knowledge.center.entity;

import ai.tx.knowledge.center.enums.ConversationsStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author tanxiong
 * @date 2025/7/18 15:41
 */
@Data
public class Conversations implements Serializable {

    private String id;

    private String userId;

    private String title;

    private String category;

    private ConversationsStatus status;

    private Integer messageCount;

    private Date lastMessageAt;

    private Date createdAt;

    private Date updatedAt;
}
