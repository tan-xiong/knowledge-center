package ai.tx.knowledge.center.repository;

import ai.tx.knowledge.center.repository.dao.ChatMessagesDAO;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

/**
 * @author tanxiong
 * @date 2025/7/18 16:06
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessagesRepository {

    ChatMessagesDAO chatMessagesDAO;
}
