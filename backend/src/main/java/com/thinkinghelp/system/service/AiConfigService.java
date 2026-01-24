package com.thinkinghelp.system.service;

import com.thinkinghelp.system.entity.AiConfig;

import java.util.List;

public interface AiConfigService {
    AiConfig getByKey(String key);
    List<AiConfig> listAll();
    AiConfig saveOrUpdate(String key, AiConfig payload);
}
