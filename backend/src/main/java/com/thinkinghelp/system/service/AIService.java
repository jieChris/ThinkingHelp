package com.thinkinghelp.system.service;

import reactor.core.publisher.Flux;

public interface AIService {

    /**
     * 标准对话 - 返回 完整响应字符串
     */
    String chat(String prompt);

    /**
     * 标准对话 - 返回 从 JSON 映射的完整响应对象
     */
    <T> T chat(String prompt, Class<T> clazz);

    /**
     * 流式对话 - 返回 字符串流 (兼容 SSE)
     */
    Flux<String> streamChat(String prompt);
}
