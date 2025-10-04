package com.gantoniadis.cargopulse.security.service.impl;

import com.gantoniadis.cargopulse.security.service.JwtBlocklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JwtBlocklistServiceImpl implements JwtBlocklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLOCKLIST_PREFIX = "blocklist:";

    /**
     * Adds a token to the blocklist with a Time-To-Live (TTL) equal to its remaining lifespan.
     * @param token The raw JWT string.
     * @param expirationDuration The time remaining until the token naturally expires.
     */
    public void blockToken(String token, Duration expirationDuration) {
        // Set the key: "blocklist:<token>" and the value "blocked".
        // The key will automatically expire when the token should have expired.
        redisTemplate.opsForValue().set(BLOCKLIST_PREFIX + token, "blocked", expirationDuration);
    }

    /**
     * Checks if a token is present in the blocklist.
     * @param token The raw JWT string.
     * @return true if the token is blocklisted.
     */
    public boolean isTokenBlocklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLOCKLIST_PREFIX + token));
    }
}
