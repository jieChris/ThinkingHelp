package com.thinkinghelp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.entity.AiConfig;
import com.thinkinghelp.system.mapper.AiConfigMapper;
import com.thinkinghelp.system.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {

    private final AiConfigMapper aiConfigMapper;

    @Override
    public AiConfig getByKey(String key) {
        return aiConfigMapper.selectOne(new LambdaQueryWrapper<AiConfig>().eq(AiConfig::getConfigKey, key));
    }

    @Override
    public List<AiConfig> listAll() {
        return aiConfigMapper.selectList(new LambdaQueryWrapper<AiConfig>().orderByAsc(AiConfig::getConfigKey));
    }

    @Override
    public AiConfig saveOrUpdate(String key, AiConfig payload) {
        AiConfig existing = getByKey(key);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            AiConfig config = new AiConfig();
            config.setConfigKey(key);
            config.setApiKey(payload.getApiKey());
            config.setBaseUrl(payload.getBaseUrl());
            config.setModel(payload.getModel());
            config.setTemperature(payload.getTemperature());
            config.setMaxTokens(payload.getMaxTokens());
            config.setEnabled(payload.getEnabled() == null ? 1 : payload.getEnabled());
            config.setCreatedAt(now);
            config.setUpdatedAt(now);
            aiConfigMapper.insert(config);
            return config;
        }
        if (payload.getApiKey() != null) {
            existing.setApiKey(payload.getApiKey());
        }
        if (payload.getBaseUrl() != null) {
            existing.setBaseUrl(payload.getBaseUrl());
        }
        if (payload.getModel() != null) {
            existing.setModel(payload.getModel());
        }
        if (payload.getTemperature() != null) {
            existing.setTemperature(payload.getTemperature());
        }
        if (payload.getMaxTokens() != null) {
            existing.setMaxTokens(payload.getMaxTokens());
        }
        if (payload.getEnabled() != null) {
            existing.setEnabled(payload.getEnabled());
        }
        existing.setUpdatedAt(now);
        aiConfigMapper.updateById(existing);
        return existing;
    }
}
