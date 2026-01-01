package com.thinkinghelp.system.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptManager {

    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个乐于助人的助手。";

    private static final String NUTRITIONIST_SYSTEM_PROMPT = "你是一个专业的慢病管理营养师。你的职责是根据用户的健康档案提供科学、精准的饮食建议。请务必基于权威医学指南回答。";

    public String buildNutritionistPrompt(String userQuestion, String context) {
        StringBuilder prompt = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            prompt.append("Context information is below.\n");
            prompt.append("---------------------\n");
            prompt.append(context).append("\n");
            prompt.append("---------------------\n");
        }
        prompt.append("User Question: ").append(userQuestion);
        return prompt.toString();
    }

    public String getSystemPrompt() {
        return NUTRITIONIST_SYSTEM_PROMPT;
    }
}
