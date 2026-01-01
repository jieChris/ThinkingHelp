package com.thinkinghelp.system.controller;

import com.thinkinghelp.system.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI 对话", description = "AI 营养师对话接口")
public class ChatController {

    private final RagService ragService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话")
    public Flux<String> streamChat(@RequestBody String question) {
        return ragService.streamAsk(question);
    }

    @PostMapping(value = "/ask")
    @Operation(summary = "普通对话")
    public String ask(@RequestBody String question) {
        return ragService.ask(question);
    }
}
