package com.whiskey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.whiskey")
@EnableJpaAuditing
@EnableScheduling
public class WhiskeyApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhiskeyApplication.class, args);
    }
}