package ai.tx.knowledge.center.service;


import ai.tx.knowledge.center.entity.ChatEntity;
import ai.tx.knowledge.center.entity.ChatMessages;
import ai.tx.knowledge.center.repository.ChatMessagesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChatStorageMemory implements ChatMemory {

    @Autowired
    private ChatMessagesRepository chatMessagesRepository;

    private static final String KEY_PREFIX = "chat:history:";
    private final RedisTemplate<String, Object> redisTemplate;

    public ChatStorageMemory(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        List<ChatEntity> listIn = new ArrayList<>();
        for (Message msg : messages) {
            String[] strs = msg.getText().split("</think>");
            String text = strs.length == 2 ? strs[1] : strs[0];

            ChatEntity ent = new ChatEntity();
            ent.setChatId(conversationId);
            ent.setType(msg.getMessageType().getValue());
            ent.setText(text);
            listIn.add(ent);
        }
        redisTemplate.opsForList().rightPushAll(key, listIn.toArray());
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
    }

    @Override
    public List<Message> get(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return Collections.emptyList();
        }

//        int start = Math.max(0, size.intValue());
        List<Object> listTmp = redisTemplate.opsForList().range(key, 0, -1);
        List<Message> listOut = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (Object obj : listTmp) {
            ChatEntity chat = objectMapper.convertValue(obj, ChatEntity.class);
            if (MessageType.USER.getValue().equals(chat.getType())) {
                listOut.add(new UserMessage(chat.getText()));
            } else if (MessageType.ASSISTANT.getValue().equals(chat.getType())) {
                listOut.add(new AssistantMessage(chat.getText()));
            } else if (MessageType.SYSTEM.getValue().equals(chat.getType())) {
                listOut.add(new SystemMessage(chat.getText()));
            }
        }
        return listOut;
    }

    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    /**
     * 会话预热
     *
     * @param conversationId
     */
    public void warmupConversation(String conversationId) {
        List<Message> messages = this.get(conversationId);
        if (CollectionUtils.isEmpty(messages)) {
            log.info("预热会话: conversationId={}", conversationId);
            List<ChatMessages> chatMessages = chatMessagesRepository.findByConversationId(conversationId);

            messages = chatMessages.stream()
                    .map(msg -> convertChatMessageToSpringAIMessage(msg))
                    .toList();
            this.add(conversationId, messages);
        }

    }


    /**
     * 强制同步会话到持久化存储
     */
    @Transactional(rollbackFor = Exception.class)
    public void forceSync(String conversationId) {
        List<Message> messages = this.get(conversationId);
        if (!CollectionUtils.isEmpty(messages)) {
            log.info("强制同步会话: conversationId={}, messageCount={}", conversationId, messages.size());
            try {
                List<ChatMessages> chatMessages = messages.stream()
                        .map(msg -> convertSpringAIMessageToChatMessage(conversationId, msg))
                        .collect(Collectors.toList());

                chatMessagesRepository.saveAll(chatMessages);

            } catch (Exception e) {
                log.error("强制同步失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            }
        }
    }


    private ChatMessages convertSpringAIMessageToChatMessage(String conversationId, Message message) {
        ChatMessages chatMessage = new ChatMessages();
        chatMessage.genId();
        chatMessage.setConversationId(conversationId);
        chatMessage.setContent(message.getText());

        // 设置消息类型
        if (message instanceof UserMessage) {
            chatMessage.setMessageType(MessageType.USER);
        } else if (message instanceof AssistantMessage) {
            chatMessage.setMessageType(MessageType.ASSISTANT);
        } else if (message instanceof SystemMessage) {
            chatMessage.setMessageType(MessageType.SYSTEM);
        }
        return chatMessage;
    }


    /**
     * 转换ChatMessage到Spring AI Message
     */
    private Message convertChatMessageToSpringAIMessage(ChatMessages chatMessages) {
        Map<String, Object> metadata = null;
        switch (chatMessages.getMessageType()) {
            case USER:
                return new UserMessage(chatMessages.getContent());
            case ASSISTANT:
                return new AssistantMessage(chatMessages.getContent());
            case SYSTEM:
                return new SystemMessage(chatMessages.getContent());
            default:
                return new UserMessage(chatMessages.getContent());
        }
    }
}


