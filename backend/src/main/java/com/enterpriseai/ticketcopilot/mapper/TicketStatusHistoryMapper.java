package com.enterpriseai.ticketcopilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterpriseai.ticketcopilot.entity.TicketStatusHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketStatusHistoryMapper extends BaseMapper<TicketStatusHistory> {
}
