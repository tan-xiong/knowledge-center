package ai.tx.knowledge.center.repository;

import ai.tx.knowledge.center.entity.Conversations;
import ai.tx.knowledge.center.model.ConversationsDO;
import ai.tx.knowledge.center.repository.dao.ConversationsDAO;
import cn.hutool.core.bean.BeanUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

/**
 * @author tanxiong
 * @date 2025/7/18 16:07
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationsRepository {

    ConversationsDAO conversationsDAO;

    public Conversations findByConversationId(String conversationId){
        ConversationsDO conversationDO = conversationsDAO.findByConversationId(conversationId);
        return BeanUtil.copyProperties(conversationDO,Conversations.class);
    }

    public void save(Conversations conversations){
        conversationsDAO.save(BeanUtil.copyProperties(conversations,ConversationsDO.class));
    }
}
