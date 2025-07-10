package ai.tx.knowledge.center.config;

import ai.tx.knowledge.center.service.ChatStorageMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author tanxiong
 * @date 2025/7/9 11:05
 */
@Configuration
public class AssistantChatConfig {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatModel chatModel;

    // 从配置文件读取提示词
    @Value("${assistant.system-prompt}")
    private String systemPrompt;

    @Bean
    public ChatClient chatClient(ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        new QuestionAnswerAdvisor(vectorStore),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    public ChatMemory chatMemory(RedisTemplate<String, Object> redisTemplate) {
        return new ChatStorageMemory(redisTemplate);
    }
}
