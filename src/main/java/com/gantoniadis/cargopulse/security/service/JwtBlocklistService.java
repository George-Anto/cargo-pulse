package com.gantoniadis.cargopulse.security.service;

import java.time.Duration;

public interface JwtBlocklistService {

    /**
     * Adds a token to the blocklist with a Time-To-Live (TTL) equal to its remaining lifespan.
     * This effectively revokes the token instantly.
     * @param token The raw JWT string.
     * @param expirationDuration The time remaining until the token naturally expires.
     */
    void blockToken(String token, Duration expirationDuration);

    /**
     * Checks if a token is present in the blocklist (i.e., if it has been revoked).
     * @param token The raw JWT string.
     * @return true if the token is blocklisted, false otherwise.
     */
    boolean isTokenBlocklisted(String token);
}