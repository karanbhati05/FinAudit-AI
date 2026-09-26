package com.finaudit.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.finaudit"})
public class FinAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinAuditApplication.class, args);
    }
}
