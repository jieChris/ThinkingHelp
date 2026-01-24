package com.thinkinghelp.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkinghelp.system.entity.AiConfig;
import com.thinkinghelp.system.service.AIService;
import com.thinkinghelp.system.service.AiConfigService;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final OpenAiChatClient chatClient;
    private final PromptManager promptManager;
    private final ObjectMapper objectMapper;
    private final AiConfigService aiConfigService;

    private final Map<String, CachedClient> clientCache = new ConcurrentHashMap<>();

    @Override
    public String chat(String userPrompt) {
        return chatClient.call(createPrompt(userPrompt)).getResult().getOutput().getContent();
    }

    @Override
    public String chat(String userPrompt, String configKey) {
        OpenAiChatClient client = resolveClient(configKey);
        return client.call(createPrompt(userPrompt)).getResult().getOutput().getContent();
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
    public <T> T chat(String userPrompt, Class<T> clazz, String configKey) {
        String jsonPrompt = userPrompt + "\n请以严格的 JSON 格式返回，不要包含 markdown 代码块标记。";
        String responseContent = chat(jsonPrompt, configKey);
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

    @Override
    public Flux<String> streamChat(String userPrompt, String configKey) {
        OpenAiChatClient client = resolveClient(configKey);
        return client.stream(createPrompt(userPrompt))
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

    private OpenAiChatClient resolveClient(String configKey) {
        if (configKey == null || configKey.isBlank()) {
            return chatClient;
        }
        AiConfig config = aiConfigService.getByKey(configKey);
        if (config == null || config.getEnabled() == null || config.getEnabled() == 0) {
            return chatClient;
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return chatClient;
        }
        CachedClient cached = clientCache.get(configKey);
        if (cached != null && cached.updatedAt != null && cached.updatedAt.equals(config.getUpdatedAt())) {
            return cached.client;
        }
        OpenAiChatClient client = buildClient(config);
        if (client == null) {
            return chatClient;
        }
        clientCache.put(configKey, new CachedClient(client, config.getUpdatedAt()));
        return client;
    }

    private OpenAiChatClient buildClient(AiConfig config) {
        try {
            Object api = buildOpenAiApi(config);
            if (api == null) return null;
            Object options = buildChatOptions(config);

            Constructor<?>[] constructors = OpenAiChatClient.class.getConstructors();
            for (Constructor<?> ctor : constructors) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 2 && params[0].isAssignableFrom(api.getClass())
                        && options != null && params[1].isAssignableFrom(options.getClass())) {
                    return (OpenAiChatClient) ctor.newInstance(api, options);
                }
            }
            for (Constructor<?> ctor : constructors) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(api.getClass())) {
                    return (OpenAiChatClient) ctor.newInstance(api);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to build dynamic OpenAiChatClient", e);
        }
        return null;
    }

    private Object buildOpenAiApi(AiConfig config) {
        try {
            Class<?> apiClass = Class.forName("org.springframework.ai.openai.OpenAiApi");
            try {
                return apiClass.getConstructor(String.class, String.class)
                        .newInstance(config.getBaseUrl(), config.getApiKey());
            } catch (NoSuchMethodException ignore) {
                return apiClass.getConstructor(String.class).newInstance(config.getApiKey());
            }
        } catch (Exception e) {
            log.warn("Failed to create OpenAiApi instance", e);
            return null;
        }
    }

    private Object buildChatOptions(AiConfig config) {
        if (config.getModel() == null && config.getTemperature() == null && config.getMaxTokens() == null) {
            return null;
        }
        try {
            Class<?> optionsClass = Class.forName("org.springframework.ai.openai.OpenAiChatOptions");
            Method builderMethod = optionsClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            invokeBuilder(builder, "withModel", config.getModel());
            if (config.getTemperature() != null) {
                invokeBuilder(builder, "withTemperature", config.getTemperature());
            }
            if (config.getMaxTokens() != null) {
                invokeBuilder(builder, "withMaxTokens", config.getMaxTokens());
            }
            Method build = builder.getClass().getMethod("build");
            return build.invoke(builder);
        } catch (Exception e) {
            log.warn("Failed to build OpenAiChatOptions", e);
            return null;
        }
    }

    private void invokeBuilder(Object builder, String method, Object value) {
        if (value == null) return;
        try {
            for (Method m : builder.getClass().getMethods()) {
                if (m.getName().equals(method) && m.getParameterCount() == 1) {
                    m.invoke(builder, value);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static class CachedClient {
        private final OpenAiChatClient client;
        private final java.time.LocalDateTime updatedAt;

        private CachedClient(OpenAiChatClient client, java.time.LocalDateTime updatedAt) {
            this.client = client;
            this.updatedAt = updatedAt;
        }
    }
}
