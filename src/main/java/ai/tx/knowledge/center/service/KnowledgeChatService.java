package ai.tx.knowledge.center.service;

import ai.tx.knowledge.center.entity.Conversations;
import ai.tx.knowledge.center.entity.Documents;
import ai.tx.knowledge.center.enums.ConversationsStatus;
import ai.tx.knowledge.center.repository.ConversationsRepository;
import ai.tx.knowledge.center.repository.DocumentsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor.TOP_K;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * @author tanxiong
 * @date 2025/7/14 15:04
 */
@Slf4j
@Service
public class KnowledgeChatService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatStorageMemory chatMemory;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DocumentsRepository documentsRepository;

    @Autowired
    private ConversationsRepository conversationsRepository;

    /**
     * 聊天对话
     */
    @Transactional(rollbackFor = Exception.class)
    public Flux<String> chat(String conversationId, String userMessage, String category) {

        List<Documents> documents = documentsRepository.findByCategory(category);
        List<String> documentIds = documents.stream().map(Documents::getId).toList();

        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        Filter.Expression documentId = filterBuilder.in("documentId", documentIds.toArray()).build();

        // 查询会话是否存在
        Conversations conversations = conversationsRepository.findByConversationId(conversationId);
        if (Objects.nonNull(conversations)) {
            // 预热会话
            chatMemory.warmupConversation(conversationId);
            // 更新最后消息时间
            conversations.setLastMessageAt(new Date());
        } else {
            //生成会话标题
            conversations = new Conversations();
            conversations.createConversations(conversationId, userMessage, category);
            conversations.setLastMessageAt(new Date());
        }

        // 历史消息列表
        List<Message> historyMessage = chatMemory.get(conversationId);

        // 发起聊天请求并处理响应
        Flux<String> chat = chatClient.prompt()
                .messages(historyMessage)
                .user(userMessage)
                .advisors(a -> a.param(CONVERSATION_ID, conversationId).param(TOP_K, 100))
                .advisors(QuestionAnswerAdvisor
                        .builder(vectorStore)
                        .searchRequest(SearchRequest.builder().filterExpression(documentId).build())
                        .build())
                .stream()
                .content()
                .doOnComplete(() -> {
                    // 7. 聊天完成后，强制同步到持久化存储
                    log.info("聊天完成，强制同步会话: {}", conversationId);
                    chatMemory.forceSync(conversationId);
                })
                .onErrorResume(throwable -> {
                    log.error("聊天服务异常", throwable);
                    return Flux.just("聊天服务暂时不可用，请稍后重试。");
                });

        conversationsRepository.save(conversations);
        return chat;
    }

    /**
     * 获取聊天历史
     */
    public List<Message> hisMessage(String conversationId) {
        return chatMemory.get(conversationId);
    }

    /**
     * 清空聊天历史
     */
    public void clearMessage(String conversationId) {
        chatMemory.clear(conversationId);
    }

    /**
     * 获取用户的会话列表
     */
    public List<Conversations> getUserConversations(String userId, ConversationsStatus status) {
        if (status == null) {
            // 如果状态为空，返回活跃和归档的会话
            return conversationsRepository.findActiveAndArchivedByUserId(userId);
        }
        return conversationsRepository.findByUserIdAndStatus(userId, status);
    }


    /**
     * 删除会话（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(String conversationId) {
        // 验证会话是否属于该用户  会话是否存在 用户是否有权操作会话
        Conversations conversation = conversationsRepository.findByConversationId(conversationId);
        conversation.setStatus(ConversationsStatus.DELETED);
        conversationsRepository.save(conversation);

        // 清理该会话的内存缓存
        chatMemory.clear(conversationId);
    }


}
