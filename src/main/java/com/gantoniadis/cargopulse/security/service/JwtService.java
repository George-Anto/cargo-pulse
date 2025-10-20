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
     * Generates a new JWT token (Access Token) for a given user with extra claims.
     * @param extraClaims Map of additional claims to include in the token payload.
     * @param userDetails The UserDetails object representing the user.
     * @return The generated JWT (Access Token) string.
     */
    String generateToken(Map<String, Object> extraClaims, UserDetails userDetails);

    /**
     * Generates a new, long-lived Refresh Token for a given user.
     * @param userDetails The UserDetails object representing the user.
     * @return The generated Refresh Token string.
     */
    String generateRefreshToken(UserDetails userDetails);

    /**
     * Validates if a token is authentic and has not expired for a given user.
     * @param token The raw JWT string.
     * @param userDetails The UserDetails object for comparison.
     * @return True if the token is valid, false otherwise.
     */
    boolean isTokenValid(String token, UserDetails userDetails);

    /**
     * Checks if the given JWT token is expired.
     * This method is often implemented to safely check the expiration without throwing an exception
     * if the token is expired but valid in terms of signature.
     * @param token The raw JWT string.
     * @return True if the token is expired, false otherwise.
     */
    boolean isTokenExpired(String token);

    /**
     * Extracts the expiration date from the JWT token.
     * @param token The raw JWT string.
     * @return The expiration Date object.
     */
    Date extractExpiration(String token);

    /**
     * Retrieves the configured expiration time (in milliseconds) for the standard Access Token.
     * @return The access token expiration time in milliseconds.
     */
    long getExpiration();

    /**
     * Retrieves the configured expiration time (in milliseconds) for the Refresh Token.
     * @return The refresh token expiration time in milliseconds.
     */
    long getRefreshExpiration();
}