package ai.tx.knowledge.center.repository;

import ai.tx.knowledge.center.entity.ChatMessages;
import ai.tx.knowledge.center.model.ChatMessagesDO;
import ai.tx.knowledge.center.repository.dao.ChatMessagesDAO;
import cn.hutool.core.bean.BeanUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author tanxiong
 * @date 2025/7/18 16:06
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessagesRepository {

    ChatMessagesDAO chatMessagesDAO;

    public List<ChatMessages> findByConversationId(String conversationId){
        List<ChatMessagesDO> chatMessagesDOS = chatMessagesDAO.findByConversationId(conversationId);
        return BeanUtil.copyToList(chatMessagesDOS,ChatMessages.class);
    }

    public void saveAll(List<ChatMessages> chatMessages){
        chatMessagesDAO.saveAll(BeanUtil.copyToList(chatMessages,ChatMessagesDO.class));
    }
}
