package com.gantoniadis.cargopulse.security.controller;

import com.gantoniadis.cargopulse.security.dto.AuthenticationRequestDTO;
import com.gantoniadis.cargopulse.security.dto.AuthenticationResponseDTO;
import com.gantoniadis.cargopulse.security.dto.LogoutRequestDTO;
import com.gantoniadis.cargopulse.security.exception.CustomAuthenticationException;
import com.gantoniadis.cargopulse.security.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling all authentication and token management operations.
 * It provides separate endpoints tailored for Web (HttpOnly Cookie) and Mobile (Header/Body) clients.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // --- LOGIN ENDPOINTS ---

    /**
     * Handles login for **Mobile** clients. Returns both Access Token (AT) and Refresh Token (RT) in the body.
     */
    @PostMapping("/login/mobile")
    public ResponseEntity<AuthenticationResponseDTO> authenticateMobile(@RequestBody AuthenticationRequestDTO request) {
        // Auth service returns both tokens in the response body
        return ResponseEntity.ok(authService.authenticateMobile(request));
    }

    /**
     * Handles login for **Web** clients. Returns AT in the body. RT is set as a secure HttpOnly cookie by the service.
     */
    @PostMapping("/login/web")
    public ResponseEntity<AuthenticationResponseDTO> authenticateWeb(
            @RequestBody AuthenticationRequestDTO request,
            HttpServletResponse response // Passed to service to set HttpOnly cookie
    ) {
        return ResponseEntity.ok(authService.authenticateWeb(request, response));
    }

    // --- REFRESH ENDPOINTS (with Refresh Token Rotation) ---

    /**
     * Handles token refresh for **Web** clients. RT is read from the HttpOnly cookie.
     */
    @PostMapping("/refresh/web")
    public ResponseEntity<AuthenticationResponseDTO> refreshWeb(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request, HttpServletResponse response) {
        // Extract RT from the incoming request cookie (sent automatically by the browser)
        String refreshToken = extractRefreshTokenFromCookie(request);
        String oldAccessToken = extractAccessToken(authHeader);

        if (refreshToken == null) {
            // Throw exception if RT is missing; handled by GlobalExceptionHandler
            throw new CustomAuthenticationException("Refresh token missing from cookie.");
        }
        // Service performs rotation, blocks old RT, and sets NEW RT cookie on the response
        return ResponseEntity.ok(authService.refreshWeb(oldAccessToken, refreshToken, response));
    }

    /**
     * Handles token refresh for **Mobile** clients. RT is read from the custom X-Refresh-Token header.
     */
    @PostMapping("/refresh/mobile")
    public ResponseEntity<AuthenticationResponseDTO> refreshMobile(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-Refresh-Token") String refreshToken // RT read from custom header
    ) {
        String oldAccessToken = extractAccessToken(authHeader);

        if (refreshToken == null || refreshToken.isEmpty()) {
            // Throw exception if RT is missing; handled by GlobalExceptionHandler
            throw new CustomAuthenticationException("Refresh token missing from X-Refresh-Token header.");
        }
        // Service performs rotation and returns new AT/RT in the response body
        return ResponseEntity.ok(authService.refreshMobile(oldAccessToken, refreshToken));
    }

    // --- LOGOUT ENDPOINT (Universal) ---

    /**
     * Handles universal logout. Determines client type based on cookie presence and delegates.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader, // Access Token header for blocklisting
            @RequestBody(required = false) LogoutRequestDTO requestBody, // Refresh Token body for mobile
            HttpServletRequest request, // To check for cookie
            HttpServletResponse response // To expire cookie
    ) {
        // Attempt to extract RT from the HttpOnly cookie (Web flow primary check)
        String refreshTokenFromCookie = extractRefreshTokenFromCookie(request);

        if (refreshTokenFromCookie != null) {
            // WEB Logout: Service revokes RT (from cookie) and handles cookie expiration
            authService.logoutWeb(authHeader, refreshTokenFromCookie, response);
        } else {
            // MOBILE Logout: Use RT from request body (if provided) for revocation
            String refreshTokenFromBody = (requestBody != null) ? requestBody.getRefreshToken() : null;
            authService.logout(authHeader, refreshTokenFromBody);
        }
        return ResponseEntity.ok().build();
    }

    // --- PRIVATE HELPER METHODS ---

    /**
     * Utility to extract the "refreshToken" value from the incoming request cookies.
     * This is used for web refresh/logout operations.
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals("refreshToken")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Extracts the raw JWT/Access Token string from the Authorization header (e.g., removes "Bearer ").
     */
    private String extractAccessToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}