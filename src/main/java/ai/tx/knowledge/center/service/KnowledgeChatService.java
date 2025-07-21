package ai.tx.knowledge.center.service;

import ai.tx.knowledge.center.entity.Documents;
import ai.tx.knowledge.center.repository.ChatMessagesRepository;
import ai.tx.knowledge.center.repository.ConversationsRepository;
import ai.tx.knowledge.center.repository.DocumentsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

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
    private ChatMemory chatMemory;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DocumentsRepository documentsRepository;

    @Autowired
    private ConversationsRepository conversationsRepository;

    @Autowired
    private ChatMessagesRepository chatMessagesRepository;

    public Flux<String> chat(String conversationId,String userMessage,String category){

        List<Documents> documents = documentsRepository.findByCategory(category);
        List<String> documentIds = documents.stream().map(Documents::getId).toList();

        FilterExpressionBuilder filterBuilder =  new FilterExpressionBuilder();
        Filter.Expression documentId = filterBuilder.in("documentId",documentIds.toArray()).build();

        // 历史消息列表
        List<Message> historyMessage = chatMemory.get(conversationId);

        // 发起聊天请求并处理响应
        return chatClient.prompt()
                .messages(historyMessage)
                .user(userMessage)
                .advisors(a -> a.param(CONVERSATION_ID, conversationId).param(TOP_K, 100))
                .advisors(QuestionAnswerAdvisor
                        .builder(vectorStore)
                        .searchRequest(SearchRequest.builder().filterExpression(documentId).build())
                        .build())
                .stream()
                .content()
                .onErrorResume(throwable -> {
                    log.error("聊天服务异常", throwable);
                    return Flux.just("聊天服务暂时不可用，请稍后重试。");
                });

    }

    public List<Message> hisMessage(String conversationId) {
        return chatMemory.get(conversationId);
    }


    public void clearMessage(String conversationId){
        chatMemory.clear(conversationId);
    }

}
