package com.finaudit.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {"com.finaudit"},
        exclude = {org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class}
)
public class FinAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinAuditApplication.class, args);
    }
}
