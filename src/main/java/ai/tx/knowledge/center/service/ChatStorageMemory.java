package ai.tx.knowledge.center.service;


import ai.tx.knowledge.center.entity.ChatMessages;
import ai.tx.knowledge.center.repository.ChatMessagesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Set;

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
        List<ChatMessages> listIn = new ArrayList<>();
        for (Message msg : messages) {
            String[] strs = msg.getText().split("</think>");
            String text = strs.length == 2 ? strs[1] : strs[0];

            ChatMessages ent = new ChatMessages();
            ent.setConversationId(conversationId);
            ent.setMessageType(msg.getMessageType());
            ent.setContent(text);
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
            ChatMessages chat = objectMapper.convertValue(obj, ChatMessages.class);
            if (MessageType.USER.equals(chat.getMessageType())) {
                listOut.add(new UserMessage(chat.getContent()));
            } else if (MessageType.ASSISTANT.equals(chat.getMessageType())) {
                listOut.add(new AssistantMessage(chat.getContent()));
            } else if (MessageType.SYSTEM.equals(chat.getMessageType())) {
                listOut.add(new SystemMessage(chat.getContent()));
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
     * 强制同步会话到持久化存储（改进版：基于内容hash的幂等同步）
     */
    @Transactional(rollbackFor = Exception.class)
    public void forceSync(String conversationId) {
        List<Message> redisMessages = this.get(conversationId);
        if (CollectionUtils.isEmpty(redisMessages)) {
            return;
        }

        // 1. 获取数据库中已有的消息
        List<ChatMessages> dbMessages = chatMessagesRepository.findByConversationId(conversationId);
        
        // 2. 构建已存在消息的内容hash集合（用于去重）
        Set<String> existingContentHashes = dbMessages.stream()
                .map(ChatMessages::getContentHash)
                .filter(hash -> hash != null && !hash.isEmpty())
                .collect(Collectors.toSet());

        log.info("同步检查: conversationId={}, Redis消息数={}, 数据库消息数={}", 
                conversationId, redisMessages.size(), dbMessages.size());

        // 3. 过滤出真正需要保存的新消息
        List<ChatMessages> newChatMessages = new ArrayList<>();
        int newMessageCount = 0;
        
        for (Message redisMsg : redisMessages) {
            String contentHash = generateContentHash(redisMsg.getText(), redisMsg.getMessageType().toString());
            
            // 如果消息内容hash不存在于数据库中，则为新消息
            if (!existingContentHashes.contains(contentHash)) {
                ChatMessages chatMessage = convertSpringAIMessageToChatMessage(conversationId, redisMsg);
                newChatMessages.add(chatMessage);
                newMessageCount++;
            }
        }

        // 4. 批量保存新消息
        if (!newChatMessages.isEmpty()) {
            chatMessagesRepository.saveAll(newChatMessages);
            log.info("增量同步完成: conversationId={}, 新增消息数={}", conversationId, newMessageCount);
        } else {
            log.debug("消息已同步，无需操作: conversationId={}", conversationId);
        }
    }

    /**
     * 生成消息内容的唯一hash（用于去重）
     */
    private String generateContentHash(String content, String messageType) {
        try {
            String input = messageType + ":" + content;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 转换为16进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.warn("MD5算法不可用，使用hashCode替代", e);
            return messageType + ":" + content.hashCode();
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
        
        // 设置内容hash（用于去重）
        chatMessage.setContentHash(generateContentHash(message.getText(), message.getMessageType().toString()));
        
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


