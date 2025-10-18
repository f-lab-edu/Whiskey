package com.whiskey.domain.order.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String EXPIRY_KEY = "order:expire";

    public void reserveExpire(Long orderId, LocalDateTime expireAt) {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        double score = expireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        zSetOperations.add(EXPIRY_KEY, String.valueOf(orderId), score);
    }

    public void removeExpire(Long orderId) {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        zSetOperations.remove(EXPIRY_KEY, String.valueOf(orderId));
    }
}
