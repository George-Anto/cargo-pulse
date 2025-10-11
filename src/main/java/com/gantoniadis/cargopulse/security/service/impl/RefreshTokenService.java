package com.gantoniadis.cargopulse.security.service.impl;

import com.gantoniadis.cargopulse.security.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    // Stores the refresh token for a user, setting Redis TTL based on JWT expiry
    public void storeRefreshToken(String userId, String refreshToken) {
        Date expirationDate = jwtService.extractExpiration(refreshToken);
        Duration ttl = Duration.between(new Date().toInstant(), expirationDate.toInstant());

        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + userId, refreshToken, ttl);
    }

    // Retrieves the currently valid refresh token for a user
    public Optional<String> getRefreshToken(String userId) {
        String token = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        return Optional.ofNullable(token);
    }

    // Revokes the refresh token (e.g., on logout or password change)
    public void deleteRefreshToken(String userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    // Validates if the token passed is the one currently stored for the user
    public boolean isValidForUser(String userId, String token) {
        Optional<String> storedToken = getRefreshToken(userId);
        // Ensure token exists, matches the stored token, and is not expired
        return storedToken.isPresent()
                && storedToken.get().equals(token)
                && !jwtService.isTokenExpired(token);
    }
}