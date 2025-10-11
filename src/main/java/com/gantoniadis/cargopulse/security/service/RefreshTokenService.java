package com.gantoniadis.cargopulse.security.service;

import java.util.Optional;

/**
 * Service interface for managing refresh tokens in Redis.
 * Provides operations for storing, retrieving, validating, and deleting refresh tokens.
 */
public interface RefreshTokenService {

    /**
     * Stores the refresh token for a user in Redis, setting TTL based on JWT expiry.
     * @param userId The unique identifier of the user.
     * @param refreshToken The refresh token to store.
     */
    void storeRefreshToken(String userId, String refreshToken);

    /**
     * Retrieves the currently valid refresh token for a user.
     * @param userId The unique identifier of the user.
     * @return An Optional containing the refresh token if it exists, empty otherwise.
     */
    Optional<String> getRefreshToken(String userId);

    /**
     * Revokes the refresh token for a user (e.g., on logout or password change).
     * @param userId The unique identifier of the user.
     */
    void deleteRefreshToken(String userId);

    /**
     * Validates if the provided token is the one currently stored for the user and is not expired.
     * @param userId The unique identifier of the user.
     * @param token The token to validate.
     * @return True if the token is valid for the user, false otherwise.
     */
    boolean isValidForUser(String userId, String token);
}