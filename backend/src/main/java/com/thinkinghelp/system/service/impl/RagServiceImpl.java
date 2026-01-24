package com.thinkinghelp.system.service.impl;

import com.thinkinghelp.system.entity.KnowledgeBase;
import com.thinkinghelp.system.mapper.KnowledgeBaseMapper;
import com.thinkinghelp.system.service.AIService;
import com.thinkinghelp.system.service.AiConfigKeys;
import com.thinkinghelp.system.service.PromptManager;
import com.thinkinghelp.system.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final AIService aiService;
    private final PromptManager promptManager;

    @Override
    public String ask(String question) {
        String context = retrieveContext(question);
        String fullPrompt = promptManager.buildNutritionistPrompt(question, context);

        String response = aiService.chat(fullPrompt, AiConfigKeys.MEDICAL);
        return appendDisclaimerIfNeeded(response, context);
    }

    @Override
    public Flux<String> streamAsk(String question) {
        String context = retrieveContext(question);
        String fullPrompt = promptManager.buildNutritionistPrompt(question, context);

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
}
