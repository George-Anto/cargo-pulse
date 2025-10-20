package com.gantoniadis.cargopulse.security.controller;

import com.gantoniadis.cargopulse.security.dto.AuthenticationRequestDTO;
import com.gantoniadis.cargopulse.security.dto.MobileAuthenticationResponseDTO;
import com.gantoniadis.cargopulse.security.dto.MobileLogoutRequestDTO;
import com.gantoniadis.cargopulse.security.dto.WebAuthenticationResponseDTO;
import com.gantoniadis.cargopulse.security.exception.CustomAuthenticationException;
import com.gantoniadis.cargopulse.security.service.AuthService;
import com.gantoniadis.cargopulse.security.util.TokenExtractionHelper;
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
     * Handles login for **Web** clients. AT and RT are set as a secure HttpOnly cookie by the service.
     */
    @PostMapping("/login/web")
    public ResponseEntity<WebAuthenticationResponseDTO> authenticateWeb(
            @RequestBody AuthenticationRequestDTO request,
            HttpServletResponse response // Passed to service to set HttpOnly cookie
    ) {
        return ResponseEntity.ok(authService.authenticateWeb(request, response));
    }

    /**
     * Handles login for **Mobile** clients. Returns both Access Token (AT) and Refresh Token (RT) in the body.
     */
    @PostMapping("/login/mobile")
    public ResponseEntity<MobileAuthenticationResponseDTO> authenticateMobile(@RequestBody AuthenticationRequestDTO request) {
        // Auth service returns both tokens in the response body
        return ResponseEntity.ok(authService.authenticateMobile(request));
    }

    // --- REFRESH ENDPOINTS (with Refresh Token Rotation) ---

    /**
     * Handles token refresh for **Web** clients. AT and RT are read from the HttpOnly cookie.
     */
    @PostMapping("/refresh/web")
    public ResponseEntity<WebAuthenticationResponseDTO> refreshWeb(HttpServletRequest request,
                                                                      HttpServletResponse response) {
        // Extract AT and RT from the incoming request cookies (sent automatically by the browser)
        String refreshToken = TokenExtractionHelper.extractRefreshTokenFromCookie(request);

        // Throw exception if RT is missing - handled by GlobalExceptionHandler
        if (refreshToken == null) {
            throw new CustomAuthenticationException("Refresh token missing from cookie.");
        }

        // Service performs rotation, blocks old RT, and sets NEW RT cookie on the response
        return ResponseEntity.ok(authService.refreshWeb(TokenExtractionHelper.extractAccessTokenFromCookie(request),
                refreshToken, response));
    }

    /**
     * Handles token refresh for **Mobile** clients. RT is read from the custom CP-Refresh-Token header.
     */
    @PostMapping("/refresh/mobile")
    public ResponseEntity<MobileAuthenticationResponseDTO> refreshMobile(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("CP-Refresh-Token") String refreshToken // RT read from custom header
    ) {
        String oldAccessToken = TokenExtractionHelper.extractAccessTokenFromHeader(authHeader);

        if (refreshToken == null || refreshToken.isEmpty()) {
            // Throw exception if RT is missing; handled by GlobalExceptionHandler
            throw new CustomAuthenticationException("Refresh token missing from CP-Refresh-Token header.");
        }
        // Service performs rotation and returns new AT/RT in the response body
        return ResponseEntity.ok(authService.refreshMobile(oldAccessToken, refreshToken));
    }

    // --- LOGOUT ENDPOINTS ---

    /**
     * Handles logout for **Web** clients. AT and RT are read from HttpOnly cookies.
     * The service logic handles potential null/expired cookies gracefully by skipping revocation.
     */
    @PostMapping("/logout/web")
    public ResponseEntity<Void> logoutWeb(HttpServletRequest request, HttpServletResponse response) {

        // Pass tokens (which may be null if cookies expired) and response for cookie cleanup
        authService.logoutWeb(TokenExtractionHelper.extractAccessTokenFromCookie(request),
                TokenExtractionHelper.extractRefreshTokenFromCookie(request), response);

        return ResponseEntity.ok().build();
    }

    /**
     * Handles logout for **Mobile** clients. AT is read from Authorization header.
     * RT is read from body.
     */
    @PostMapping("/logout/mobile")
    public ResponseEntity<Void> logoutMobile(
            @RequestHeader(name = "Authorization") String authHeader,
            @RequestBody MobileLogoutRequestDTO requestBody) {

        // Validate Authorization Header (AT)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new CustomAuthenticationException("Authorization header must contain a 'Bearer' Access Token for mobile logout.");
        }
        // RT is required for mobile logout to ensure session revocation
        String refreshToken = requestBody.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new CustomAuthenticationException("Refresh token is required in the body for mobile logout.");
        }

        // Pass the Authorization header (for AT blocklisting) and the RT
        authService.logoutMobile(authHeader, refreshToken);

        return ResponseEntity.ok().build();
    }
}