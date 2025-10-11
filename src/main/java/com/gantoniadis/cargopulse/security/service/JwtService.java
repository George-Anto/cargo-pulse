package com.gantoniadis.cargopulse.security.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

public interface JwtService {

    /**
     * Extracts the username (subject) from the JWT token.
     * @param token The raw JWT string.
     * @return The username.
     */
    String extractUsername(String token);

    /**
     * Extracts a specific claim from the JWT token using a claims resolver function.
     * @param token The raw JWT string.
     * @param claimsResolver Function to resolve the desired claim.
     * @param <T> The type of the claim.
     * @return The resolved claim value.
     */
    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    /**
     * Generates a new JWT token for a given user with extra claims.
     * @param extraClaims Map of additional claims to include in the token payload.
     * @param userDetails The UserDetails object representing the user.
     * @return The generated JWT string.
     */
    String generateToken(Map<String, Object> extraClaims, UserDetails userDetails);

    String generateRefreshToken(UserDetails userDetails);

    /**
     * Validates if a token is authentic and has not expired for a given user.
     * @param token The raw JWT string.
     * @param userDetails The UserDetails object for comparison.
     * @return True if the token is valid, false otherwise.
     */
    boolean isTokenValid(String token, UserDetails userDetails);

    boolean isTokenExpired(String token);

    /**
     * Extracts the expiration date from the JWT token.
     * @param token The raw JWT string.
     * @return The expiration Date object.
     */
    Date extractExpiration(String token);

    long getRefreshExpiration();
}
