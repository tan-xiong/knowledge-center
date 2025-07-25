package ai.tx.knowledge.center.entity;

import ai.tx.knowledge.center.common.IdUtils;
import ai.tx.knowledge.center.enums.ConversationsStatus;
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

    private String conversationId;

    private String userId;

    private String title;

    private String category;

    private ConversationsStatus status;

    private Date lastMessageAt;

    private Date createdAt;

    private Date updatedAt;

    /**
     * 生成唯一ID
     */
    public void genId() {
        this.id = IdUtils.generateId();
    }

    /**
     * 创建对话实例
     */
    public Conversations createConversations(String conversationId, String userMessage, String category) {
        this.genId();
        this.conversationId = conversationId;
        this.status = ConversationsStatus.ACTIVE;
        this.title = generateConversationTitle(userMessage);
        this.category = category;
        this.userId = "default";
        return this;
    }

    /**
     * 生成对话标题
     */
    private String generateConversationTitle(String userMessage) {
        // 简单的标题生成逻辑：取前20个字符
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "新对话";
        }

        String cleanMessage = userMessage.trim().replaceAll("\\s+", " ");
        if (cleanMessage.length() <= 20) {
            return cleanMessage;
        }

        return cleanMessage.substring(0, 20) + "...";
    }

}
