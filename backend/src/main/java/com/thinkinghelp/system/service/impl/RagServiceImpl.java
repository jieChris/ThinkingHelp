package com.thinkinghelp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkinghelp.system.entity.HealthProfile;
import com.thinkinghelp.system.entity.KnowledgeBase;
import com.thinkinghelp.system.mapper.HealthProfileMapper;
import com.thinkinghelp.system.mapper.KnowledgeBaseMapper;
import com.thinkinghelp.system.service.AIService;
import com.thinkinghelp.system.service.AiConfigKeys;
import com.thinkinghelp.system.service.PromptManager;
import com.thinkinghelp.system.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final HealthProfileMapper healthProfileMapper;
    private final AIService aiService;
    private final PromptManager promptManager;
    private final ObjectMapper objectMapper;

    @Override
    public String ask(Long userId, String question) {
        String context = retrieveContext(question);
        String profileSummary = buildProfileSummary(userId);
        String fullPrompt = promptManager.buildNutritionistPrompt(question, context, profileSummary);

        String response = aiService.chat(fullPrompt, AiConfigKeys.MEDICAL);
        return appendDisclaimerIfNeeded(response, context);
    }

    @Override
    public Flux<String> streamAsk(Long userId, String question) {
        String context = retrieveContext(question);
        String profileSummary = buildProfileSummary(userId);
        String fullPrompt = promptManager.buildNutritionistPrompt(question, context, profileSummary);

        Flux<String> streamResponse = aiService.streamChat(fullPrompt, AiConfigKeys.MEDICAL);

        // 如果需要，将免责声明追加到流的末尾
        if (context.isEmpty()) {
            streamResponse = streamResponse.concatWith(Flux.just("\n\n(注意：本地知识库未收录相关内容，回答仅供参考)"));
        }
        // 标记流结束，便于前端主动终止读取
        return streamResponse.concatWith(Flux.just("[DONE]"));
    }

    private String retrieveContext(String question) {
        // 简单的关键字提取 (朴素实现)
        // 实际上，除了向量嵌入，我们可能会使用 LLM 来提取关键字
        String keyword = question.length() > 2 ? question.substring(0, 2) : question;

        // 尝试找到更好的提取简单关键字的方法，如果可能的话，或者如果使用全文搜索，直接传递整个问题
        // 这里我们直接使用问题进行 LIKE %question% 查询，如果问题很长会有风险。
        // 暂时假设简单的切分，或者如果是短问题，使用整个字符串。

        List<KnowledgeBase> results = knowledgeBaseMapper.searchByKeyword(question); // 尝试精确匹配重叠
        if (results.isEmpty()) {
            // 降级: 尝试使用前几个字符作为关键字?
            // 理想情况下我们需要更好的搜索策略。
            // 对于此 MVP，我们假设用户询问具体的关键字或我们运气好。
            // 或者我们什么也没找到。
        }

        if (results.isEmpty()) {
            return "";
        }

        return results.stream()
                .map(kb -> String.format("Content: %s (Source: %s)", kb.getContent(), kb.getSource()))
                .collect(Collectors.joining("\n"));
    }

    private String appendDisclaimerIfNeeded(String response, String context) {
        if (context == null || context.isEmpty()) {
            return response + "\n\n(注意：本地知识库未收录相关内容，回答仅供参考)";
        }
        return response;
    }

    private String buildProfileSummary(Long userId) {
        if (userId == null) {
            return "";
        }
        HealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>().eq(HealthProfile::getUserId, userId));
        if (profile == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        append(parts, "姓名", profile.getName());
        append(parts, "性别", profile.getGender());
        append(parts, "年龄", profile.getAge());
        append(parts, "身高(cm)", profile.getHeight());
        append(parts, "体重(kg)", profile.getWeight());
        append(parts, "BMI", profile.getBmi());
        append(parts, "体检日期", profile.getReportDate());
        append(parts, "活动水平", profile.getActivityLevel());
        append(parts, "目标", profile.getGoal());
        append(parts, "运动频率(次/周)", profile.getExerciseFrequency());
        append(parts, "单次运动时长(分钟)", profile.getExerciseDuration());

        append(parts, "收缩压(mmHg)", profile.getBpSystolic());
        append(parts, "舒张压(mmHg)", profile.getBpDiastolic());
        append(parts, "空腹血糖(mmol/L)", profile.getFastingGlucose());
        append(parts, "糖化血红蛋白(%)", profile.getHba1c());
        append(parts, "总胆固醇(mmol/L)", profile.getTotalCholesterol());
        append(parts, "甘油三酯(mmol/L)", profile.getTriglycerides());
        append(parts, "高密度脂蛋白HDL(mmol/L)", profile.getHdl());
        append(parts, "低密度脂蛋白LDL(mmol/L)", profile.getLdl());
        append(parts, "尿酸(umol/L)", profile.getUricAcid());
        append(parts, "ALT(U/L)", profile.getAlt());
        append(parts, "AST(U/L)", profile.getAst());
        append(parts, "肌酐(umol/L)", profile.getCreatinine());
        append(parts, "尿素氮(mmol/L)", profile.getBun());

        append(parts, "慢病", parseList(profile.getDiseases()));
        append(parts, "过敏", parseList(profile.getAllergies()));
        append(parts, "忌口/其他限制", profile.getOtherRestrictions());

        if (parts.isEmpty()) {
            return "";
        }
        return String.join("\n", parts);
    }

    private void append(List<String> parts, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return;
        }
        parts.add(label + ": " + text);
    }

    private String parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            List<String> values = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            if (values == null || values.isEmpty()) {
                return "";
            }
            return values.stream().filter(item -> item != null && !item.isBlank()).collect(Collectors.joining("、"));
        } catch (Exception ignored) {
            return raw;
        }
    }
}
