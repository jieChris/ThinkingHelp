package com.thinkinghelp.system.controller;

import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.AiConfig;
import com.thinkinghelp.system.service.AIService;
import com.thinkinghelp.system.service.AiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai-config")
@RequiredArgsConstructor
@Tag(name = "AI 配置", description = "后台 AI 配置管理")
public class AiConfigController {

    private final AiConfigService aiConfigService;
    private final AIService aiService;

    @GetMapping
    @Operation(summary = "获取 AI 配置列表")
    public Result<List<AiConfig>> listConfigs() {
        return Result.success(aiConfigService.listAll());
    }

    @GetMapping("/{key}")
    @Operation(summary = "获取指定 AI 配置")
    public Result<AiConfig> getConfig(@PathVariable String key) {
        AiConfig config = aiConfigService.getByKey(key);
        if (config == null) {
            return Result.success(null);
        }
        return Result.success(config);
    }

    @PutMapping("/{key}")
    @Operation(summary = "保存 AI 配置")
    public Result<AiConfig> updateConfig(@PathVariable String key, @RequestBody AiConfig payload) {
        return Result.success(aiConfigService.saveOrUpdate(key, payload));
    }

    @PostMapping("/{key}/test")
    @Operation(summary = "测试 AI 配置")
    public Result<String> testConfig(@PathVariable String key, @RequestParam(required = false) String prompt) {
        String testPrompt = (prompt == null || prompt.isBlank())
                ? "请用一句话回复：配置测试成功"
                : prompt;
        try {
            String response = aiService.chat(testPrompt, key);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("测试失败：" + e.getMessage());
        }
    }
}
