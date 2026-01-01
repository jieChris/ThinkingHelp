package com.thinkinghelp.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkinghelp.system.service.AIService;
import com.thinkinghelp.system.service.PromptManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final OpenAiChatClient chatClient;
    private final PromptManager promptManager;
    private final ObjectMapper objectMapper;

    @Override
    public String chat(String userPrompt) {
        return chatClient.call(createPrompt(userPrompt)).getResult().getOutput().getContent();
    }

    @Override
    public <T> T chat(String userPrompt, Class<T> clazz) {
        // 如果需要，在 Prompt 中强制要求 JSON 格式，或依赖 System Prompt 的指令
        // 这里我们追加一条指令以强制 JSON 返回
        String jsonPrompt = userPrompt + "\n请以严格的 JSON 格式返回，不要包含 markdown 代码块标记。";
        String responseContent = chat(jsonPrompt);

        // 清理 markdown 代码块 (如果存在 ```json ... ```)
        String cleanJson = cleanJson(responseContent);

        try {
            return objectMapper.readValue(cleanJson, clazz);
        } catch (JsonProcessingException e) {
            log.error("解析 AI 响应为 JSON 失败", e);
            throw new RuntimeException("AI 响应解析错误");
        }
    }

    @Override
    public Flux<String> streamChat(String userPrompt) {
        return chatClient.stream(createPrompt(userPrompt))
                .map(chatResponse -> {
                    if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                        return chatResponse.getResult().getOutput().getContent();
                    }
                    return "";
                });
    }

    private Prompt createPrompt(String userText) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(promptManager.getSystemPrompt()));
        messages.add(new UserMessage(userText));
        return new Prompt(messages);
    }

    private String cleanJson(String content) {
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }
}
