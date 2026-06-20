package com.enterpriseai.ticketcopilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.enterpriseai.ticketcopilot.mapper")
public class EnterpriseAiTicketCopilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseAiTicketCopilotApplication.class, args);
    }
}
