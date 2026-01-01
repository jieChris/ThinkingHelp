package com.thinkinghelp.system.service;

import reactor.core.publisher.Flux;

public interface RagService {

    /**
     * RAG 支持的问答 (标准响应)
     */
    String ask(String question);

    /**
     * RAG 支持的问答 (流式响应)
     */
    Flux<String> streamAsk(String question);
}
