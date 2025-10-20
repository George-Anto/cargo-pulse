package com.gantoniadis.cargopulse.security.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

/**
 * Utility class for extracting JWT and Refresh Tokens from common HTTP request sources:
 * Authorization header and HttpOnly cookies.
 */
// Lombok annotation for utility class (private constructor, final methods)
@UtilityClass
public class TokenExtractionHelper {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Extracts the raw JWT/Access Token string from the Authorization header (e.g., removes "Bearer ").
     *
     * @param authHeader The full Authorization header value (e.g., "Bearer <token>").
     * @return The raw Access Token, or null if the header is null, empty, or lacks the "Bearer" prefix.
     */
    public String extractAccessTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * Utility to extract the raw Access Token value from the incoming request cookies.
     *
     * @param request The HttpServletRequest.
     * @return The raw Access Token value from the "accessToken" cookie, or null if not found.
     */
    public String extractAccessTokenFromCookie(HttpServletRequest request) {
        return extractCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    /**
     * Utility to extract the raw Refresh Token value from the incoming request cookies.
     *
     * @param request The HttpServletRequest.
     * @return The raw Refresh Token value from the "refreshToken" cookie, or null if not found.
     */
    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        return extractCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
    }

    /**
     * Generic helper to search for a specific cookie by name.
     */
    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}