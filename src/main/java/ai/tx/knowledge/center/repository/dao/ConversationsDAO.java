package ai.tx.knowledge.center.repository.dao;

import ai.tx.knowledge.center.enums.ConversationsStatus;
import ai.tx.knowledge.center.model.ConversationsDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author tanxiong
 * @date 2025/7/18 15:59
 */
@Repository
public interface ConversationsDAO extends JpaRepository<ConversationsDO, String> {

    /**
     * 根据会话ID查询会话
     */
    ConversationsDO findByConversationId(String conversationId);

    /**
     * 根据用户ID和状态查询会话列表
     */
    List<ConversationsDO> findByUserIdAndStatus(String userId, ConversationsStatus status);

    /**
     * 根据用户ID查询所有活跃和归档的会话
     */
    List<ConversationsDO> findByUserIdAndStatusIn(String userId, List<ConversationsStatus> statuses);

}
