package com.whiskey.domain.order.scheduler;

import com.whiskey.domain.order.service.OrderService;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireCheckScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final OrderService orderService;

    private static final String EXPIRY_KEY = "order:expire";

    @Scheduled(fixedDelay = 60000)
    public void expireCheck() {
        log.info(">>> 스케줄러 실행! 시각: {}", java.time.LocalDateTime.now());
        Set<String> orderIds = getOrderIds();
        log.info(">>> 만료 대상 주문 수: {}", orderIds.size());

        if(orderIds.isEmpty()) {
            log.debug("만료 대상 주문 없음");
            return;
        }

        for(String orderId : orderIds) {
            expireOrder(Long.parseLong(orderId));
        }
    }

    private Set<String> getOrderIds() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        long nowTime = System.currentTimeMillis() / 1000;

        return zSetOperations.rangeByScore(EXPIRY_KEY, 0, nowTime);
    }

    private void expireOrder(long orderId) {
        try {
            orderService.expireOrder(orderId);

            ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
            zSetOperations.remove(EXPIRY_KEY, String.valueOf(orderId));
        }
        catch(Exception e) {
            log.error(e.getMessage());
        }
    }
}
