package ai.tx.knowledge.center.repository.dao;

import ai.tx.knowledge.center.model.ChatMessagesDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author tanxiong
 * @date 2025/7/18 15:59
 */
@Repository
public interface ChatMessagesDAO extends JpaRepository<ChatMessagesDO,String> {

    List<ChatMessagesDO> findByConversationId(String conversationId);

}
