package com.whiskey.domain.auth.service;

import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final StringRedisTemplate stringRedisTemplate;

    public void invalidate(long memberId) {
        String activeTokenKey = "user:" + memberId + ":active";
        Set<String> activeTokens = stringRedisTemplate.opsForSet().members(activeTokenKey);

        if(activeTokens != null && !activeTokens.isEmpty()) {
            for(String token : activeTokens) {
                String blackListKey = "blacklist:token:" + token;
                stringRedisTemplate.opsForSet().add(blackListKey, "blocked");
                stringRedisTemplate.expire(blackListKey, Duration.ofMinutes(10));
            }

            stringRedisTemplate.delete(activeTokenKey);
        }
    }

    public void registerActiveToken(long memberId, String activeToken) {
        String activeTokenKey = "user:" + memberId + ":active";

        stringRedisTemplate.opsForSet().add(activeTokenKey, activeToken);
    }

    public void addBlackList(String token) {
        String activeTokenKey = "user:" + token + ":active";
        String blackListKey = "user:" + token + ":blacklist";

        stringRedisTemplate.opsForSet().remove(activeTokenKey, token);

        stringRedisTemplate.opsForSet().add(blackListKey, token);
        stringRedisTemplate.expire(blackListKey, Duration.ofMinutes(10));
    }

    public boolean isBlackListed(String token) {
        String blackListKey = "blacklist:token:" + token;

        boolean blackListed = stringRedisTemplate.hasKey(blackListKey);
        return Boolean.TRUE.equals(blackListed);
    }
}