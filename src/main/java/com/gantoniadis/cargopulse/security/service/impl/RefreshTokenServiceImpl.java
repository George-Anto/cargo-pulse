package com.gantoniadis.cargopulse.security.service.impl;

import com.gantoniadis.cargopulse.security.service.JwtService;
import com.gantoniadis.cargopulse.security.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * Implementation of RefreshTokenService for managing refresh tokens in Redis.
 * This service handles the storage, retrieval, validation, and deletion of refresh tokens
 * with automatic TTL management based on JWT expiration times.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeRefreshToken(String userId, String refreshToken) {
        Date expirationDate = jwtService.extractExpiration(refreshToken);
        Duration ttl = Duration.between(new Date().toInstant(), expirationDate.toInstant());

        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + userId, refreshToken, ttl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getRefreshToken(String userId) {
        String token = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        return Optional.ofNullable(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteRefreshToken(String userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidForUser(String userId, String token) {
        Optional<String> storedToken = getRefreshToken(userId);
        // Ensure token exists, matches the stored token, and is not expired
        return storedToken.isPresent()
                && storedToken.get().equals(token)
                && !jwtService.isTokenExpired(token);
    }
}