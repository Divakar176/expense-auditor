package com.company.expense_auditor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.company")
@EnableJpaRepositories(basePackages = "com.company.repository")
@EntityScan(basePackages = "com.company.model")
public class ExpenseAuditorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseAuditorApplication.class, args);
    }
}