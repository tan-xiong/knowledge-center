package ai.tx.knowledge.center.controller;

import ai.tx.knowledge.center.common.Result;
import ai.tx.knowledge.center.common.ResultCode;
import ai.tx.knowledge.center.service.KnowledgeChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 知识聊天控制器
 */
@RequestMapping("/assistant")
@RestController
public class KnowledgeChatController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeChatController.class);

    @Autowired
    private KnowledgeChatService knowledgeChatService;

    /**
     * 流式聊天接口
     */
    @GetMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam(name = "conversationId") String conversationId,
                             @RequestParam(name = "userMessage") String userMessage,
                             @RequestParam(name = "category") String category) {
        try {
            // 参数验证
            if (!StringUtils.hasText(conversationId)) {
                return Flux.just("data: " + Result.error(ResultCode.INVALID_CHAT_ID.getCode(),
                        ResultCode.INVALID_CHAT_ID.getMessage()).toString() + "\n\n");
            }

            if (!StringUtils.hasText(userMessage)) {
                return Flux.just("data: " + Result.error(ResultCode.EMPTY_MESSAGE.getCode(),
                        ResultCode.EMPTY_MESSAGE.getMessage()).toString() + "\n\n");
            }

            log.info("开始聊天 - conversationId: {}, userMessage: {}", conversationId, userMessage);

            return knowledgeChatService.chat(conversationId, userMessage, category);

        } catch (Exception e) {
            log.error("聊天接口异常", e);
            return Flux.just("data: " + Result.error(ResultCode.CHAT_ERROR.getCode(),
                    "聊天服务异常: " + e.getMessage()).toString() + "\n\n");
        }
    }

    /**
     * 获取聊天历史
     */
    @GetMapping("/messages")
    public Result<List<Message>> messages(@RequestParam(value = "conversationId") String conversationId) {
        try {
            // 参数验证
            if (!StringUtils.hasText(conversationId)) {
                return Result.error(ResultCode.INVALID_CHAT_ID.getCode(),
                        ResultCode.INVALID_CHAT_ID.getMessage());
            }

            List<Message> messages = knowledgeChatService.hisMessage(conversationId);
            log.info("获取聊天历史 - conversationId: {}, 消息数量: {}", conversationId, messages.size());

            return Result.success("获取聊天历史成功", messages);

        } catch (Exception e) {
            log.error("获取聊天历史失败", e);
            return Result.error(ResultCode.CHAT_ERROR.getCode(),
                    "获取聊天历史失败: " + e.getMessage());
        }
    }

    /**
     * 清空聊天历史
     */
    @DeleteMapping("/messages")
    public Result<Object> clearMessages(@RequestParam(value = "conversationId") String conversationId) {
        try {
            // 参数验证
            if (!StringUtils.hasText(conversationId)) {
                return Result.error(ResultCode.INVALID_CHAT_ID.getCode(),
                        ResultCode.INVALID_CHAT_ID.getMessage());
            }

            knowledgeChatService.clearMessage(conversationId);
            log.info("清空聊天历史 - conversationId: {}", conversationId);

            return Result.success("清空聊天历史成功");

        } catch (Exception e) {
            log.error("清空聊天历史失败", e);
            return Result.error(ResultCode.CHAT_ERROR.getCode(),
                    "清空聊天历史失败: " + e.getMessage());
        }
    }
}
