package com.whiskey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.whiskey")
public class WhiskeyApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhiskeyApplication.class, args);
    }
}