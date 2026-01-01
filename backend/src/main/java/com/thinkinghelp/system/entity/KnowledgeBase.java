package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("knowledge_base")
public class KnowledgeBase {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    // 暂时存储为 JSON 字符串，表示向量
    // 在简单实现中，我们可能不会直接在 SQL 查询中使用此字段
    private String vector;

    private String source;
}
