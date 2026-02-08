package com.thinkinghelp.system.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptManager {

    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个乐于助人的助手。";

    private static final String NUTRITIONIST_SYSTEM_PROMPT = "你是一个专业的慢病管理营养师。你的职责是根据用户的健康档案提供科学、精准的饮食建议。请务必基于权威医学指南回答。";

    public String buildNutritionistPrompt(String userQuestion, String context, String profileSummary) {
        StringBuilder prompt = new StringBuilder();
        if (profileSummary != null && !profileSummary.isBlank()) {
            prompt.append("User Health Profile Summary is below.\n");
            prompt.append("---------------------\n");
            prompt.append(profileSummary).append("\n");
            prompt.append("---------------------\n");
        }
        if (context != null && !context.isEmpty()) {
            prompt.append("Context information is below.\n");
            prompt.append("---------------------\n");
            prompt.append(context).append("\n");
            prompt.append("---------------------\n");
        }
        prompt.append("请默认基于上述用户档案回答，用户无需重复提供个人情况；如用户本轮消息提供了更新信息且与档案冲突，以本轮消息为准并简要说明。");
        prompt.append("\nUser Question: ").append(userQuestion);
        return prompt.toString();
    }

    public String buildNutritionistPrompt(String userQuestion, String context) {
        return buildNutritionistPrompt(userQuestion, context, null);
    }

    public String getSystemPrompt() {
        return NUTRITIONIST_SYSTEM_PROMPT;
    }
}
