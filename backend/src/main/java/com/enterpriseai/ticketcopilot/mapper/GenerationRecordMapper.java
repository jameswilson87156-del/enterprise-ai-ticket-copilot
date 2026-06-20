package com.enterpriseai.ticketcopilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterpriseai.ticketcopilot.entity.GenerationRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GenerationRecordMapper extends BaseMapper<GenerationRecord> {
}
