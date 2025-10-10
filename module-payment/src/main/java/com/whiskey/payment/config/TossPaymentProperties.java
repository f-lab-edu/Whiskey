package com.whiskey.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "toss.payment")
public class TossPaymentProperties {
    private String secretKey;
    private String baseUrl;
    private Integer connectTimeout;
    private Integer readTimeout;
}
