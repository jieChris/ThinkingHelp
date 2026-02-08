package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.mapper.UserMapper;
import com.thinkinghelp.system.service.RagService;
import com.thinkinghelp.system.utils.JwtUtils;
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
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话")
    public Flux<String> streamChat(@RequestHeader(value = "Authorization", required = false) String token,
                                   @RequestBody String question) {
        Long userId = getUserIdFromToken(token);
        return ragService.streamAsk(userId, question);
    }

    @PostMapping(value = "/ask")
    @Operation(summary = "普通对话")
    public String ask(@RequestHeader(value = "Authorization", required = false) String token,
                      @RequestBody String question) {
        Long userId = getUserIdFromToken(token);
        return ragService.ask(userId, question);
    }

    private Long getUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            String username = jwtUtils.extractUsername(token);
            if (username == null || username.isBlank()) {
                return null;
            }
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            return user == null ? null : user.getId();
        } catch (Exception e) {
            return null;
        }
    }
}
