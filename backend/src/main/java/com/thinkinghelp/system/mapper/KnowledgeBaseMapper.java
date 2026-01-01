package com.thinkinghelp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thinkinghelp.system.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    // 简单的关键字搜索演示
    // 在真实的向量数据库场景中，这将是向量相似度搜索
    @Select("SELECT * FROM knowledge_base WHERE content LIKE CONCAT('%', #{keyword}, '%') LIMIT 5")
    List<KnowledgeBase> searchByKeyword(String keyword);
}
