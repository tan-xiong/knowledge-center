package ai.tx.knowledge.center.repository;

import ai.tx.knowledge.center.entity.Conversations;
import ai.tx.knowledge.center.enums.ConversationsStatus;
import ai.tx.knowledge.center.model.ConversationsDO;
import ai.tx.knowledge.center.repository.dao.ConversationsDAO;
import cn.hutool.core.bean.BeanUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author tanxiong
 * @date 2025/7/18 16:07
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationsRepository {

    ConversationsDAO conversationsDAO;

    /**
     * 根据会话ID查询会话
     */
    public Conversations findByConversationId(String conversationId) {
        ConversationsDO conversationDO = conversationsDAO.findByConversationId(conversationId);
        return conversationDO != null ? BeanUtil.copyProperties(conversationDO, Conversations.class) : null;
    }

    /**
     * 保存会话
     */
    public void save(Conversations conversations) {
        conversationsDAO.save(BeanUtil.copyProperties(conversations, ConversationsDO.class));
    }

    /**
     * 根据用户ID和状态查询会话列表
     */
    public List<Conversations> findByUserIdAndStatus(String userId, ConversationsStatus status) {
        List<ConversationsDO> conversationDOs = conversationsDAO.findByUserIdAndStatus(userId, status);
        return conversationDOs.stream()
                .map(conversationDO -> BeanUtil.copyProperties(conversationDO, Conversations.class))
                .collect(Collectors.toList());
    }


    /**
     * 根据用户ID查询活跃和归档的会话
     */
    public List<Conversations> findActiveAndArchivedByUserId(String userId) {
        List<ConversationsStatus> statuses = Arrays.asList(ConversationsStatus.ACTIVE, ConversationsStatus.ARCHIVED);
        List<ConversationsDO> conversationDOs = conversationsDAO.findByUserIdAndStatusIn(userId, statuses);
        return conversationDOs.stream()
                .map(conversationDO -> BeanUtil.copyProperties(conversationDO, Conversations.class))
                .collect(Collectors.toList());
    }
}
