package com.thinkinghelp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thinkinghelp.system.entity.HealthRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthRecordMapper extends BaseMapper<HealthRecord> {
}
