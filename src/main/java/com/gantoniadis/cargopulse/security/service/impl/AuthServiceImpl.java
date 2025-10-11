package com.gantoniadis.cargopulse.security.service.impl;

import com.gantoniadis.cargopulse.security.dto.AuthenticationRequestDTO;
import com.gantoniadis.cargopulse.security.dto.AuthenticationResponseDTO;
import com.gantoniadis.cargopulse.security.exception.CustomAuthenticationException;
import com.gantoniadis.cargopulse.security.service.AuthService;
import com.gantoniadis.cargopulse.security.service.JwtBlocklistService;
import com.gantoniadis.cargopulse.security.service.JwtService;
import com.gantoniadis.cargopulse.user.dto.UserAccountDTO;
import com.gantoniadis.cargopulse.user.mapper.UserAccountMapper;
import com.gantoniadis.cargopulse.user.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;
    private final JwtBlocklistService blocklistService;
    private final RefreshTokenService refreshTokenService;

    private final UserAccountMapper userAccountMapper;

    private final UserAccountRepository userAccountRepository;

    @Value("${security.cookie.transfer}")
    private boolean isSecureCookieTransferEnabled;

    // Core authentication logic: authenticates user, generates AT and RT, stores RT in Redis.
    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO request) throws CustomAuthenticationException {
        try {
            // Attempt to authenticate user credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new CustomAuthenticationException("User '" + request.getUsername() + "' could not be authenticated");
        }

        // Fetch user details
        var user = userAccountRepository.findByUsername(request.getUsername()).orElseThrow(
                () -> new CustomAuthenticationException("User " + request.getUsername() + " not found in the database")
        );
        var userDTO = userAccountMapper.userToUserDTO(user);
        log.info("Successful authentication of user '{}' with roles: {}", userDTO.getUsername(), userDTO.getRolesNames());

        // Generate Access Token and Refresh Token
        var accessToken = jwtService.generateToken(Collections.singletonMap("roles", userDTO.getRoles()), userDTO);
        var refreshToken = jwtService.generateRefreshToken(userDTO); // Generate the long-lived RT

        // Store the new Refresh Token in Redis (linked to user ID)
        String userId = userDTO.getId().toString();
        refreshTokenService.storeRefreshToken(userId, refreshToken);

        // Return both tokens - web auth caller will remove it
        return AuthenticationResponseDTO.builder()
                .jwt(accessToken)
                .refreshToken(refreshToken)
                .username(userDTO.getUsername())
                .roles(userDTO.getRoles())
                .build();
    }

    public UserAccountDTO getAuthenticatedUser() {
        try {
            // Retrieve token/username from the security context
            String token = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
            String username = jwtService.extractUsername(token);
            return userAccountMapper.userToUserDTO(userAccountRepository.findByUsername(username).
                    orElseThrow(() ->
                            new CustomAuthenticationException("No authorized User found for this action.")));
        } catch (Exception e) {
            throw new CustomAuthenticationException("No authorized User found for this action.");
        }
    }

    // MOBILE Login: Calls the core logic and returns the DTO with both tokens.
    @Override
    public AuthenticationResponseDTO authenticateMobile(AuthenticationRequestDTO request) {
        return authenticate(request);
    }

    // WEB Login: Calls core logic, sets HttpOnly cookie, and cleans DTO for response body.
    @Override
    public AuthenticationResponseDTO authenticateWeb(AuthenticationRequestDTO request,
                                                     HttpServletResponse response) {
        // Get tokens from core logic
        AuthenticationResponseDTO responseDTO = authenticate(request);

        // Set the Refresh Token as a secure HttpOnly cookie (XSS defense)
        setRefreshCookie(response, responseDTO.getRefreshToken());

        // Remove RT from body DTO (RT is now in cookie, AT is in body)
        responseDTO.setRefreshToken(null);
        return responseDTO;
    }

    // MOBILE Refresh: Calls the rotation logic. RT remains in the DTO body for mobile client.
    @Override
    public AuthenticationResponseDTO refreshMobile(String oldAccessToken, String oldRefreshToken) {
        return rotateRefreshToken(oldAccessToken, oldRefreshToken);
    }

    // WEB Refresh: Calls rotation logic, sets NEW HttpOnly cookie, and cleans DTO.
    @Override
    public AuthenticationResponseDTO refreshWeb(String oldAccessToken, String oldRefreshToken, HttpServletResponse response) {
        // Core rotation logic (generates new AT/RT, blocks old RT in Redis)
        AuthenticationResponseDTO responseDTO = rotateRefreshToken(oldAccessToken, oldRefreshToken);

        // Set the NEW Refresh Token as a secure HttpOnly cookie
        setRefreshCookie(response, responseDTO.getRefreshToken());

        // Remove RT from body DTO
        responseDTO.setRefreshToken(null);
        return responseDTO;
    }

    // Universal Logout: Logic to block AT and revoke RT from Redis.
    @Override
    public void logout(String authHeader, String refreshToken) {

        // Block the Access Token (AT)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            try {
                // Add short-lived AT to Redis blocklist until its natural expiry
                Date expiration = jwtService.extractExpiration(accessToken);
                long remainingMillis = expiration.getTime() - System.currentTimeMillis();
                if (remainingMillis > 0) {
                    blocklistService.blockToken(accessToken, Duration.ofMillis(remainingMillis));
                }
            }  catch (Exception ex) {
                log.warn("Error processing Access Token during logout: {}", ex.getMessage());
            }
        }
        // Revoke the Refresh Token (RT)
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                // Delete RT from Redis to instantly revoke access
                String username = jwtService.extractUsername(refreshToken);
                var user = userAccountRepository.findByUsername(username);
                if (user.isPresent()) {
                    String userId = user.get().getId().toString();
                    refreshTokenService.deleteRefreshToken(userId);
                }
            } catch (Exception ex) {
                log.warn("Error processing Refresh Token during logout: {}", ex.getMessage());
            }
        }
        // Clear the Security Context
        SecurityContextHolder.clearContext();
    }

    // WEB Logout: Calls base logout and handles cookie expiration.
    @Override
    public void logoutWeb(String authHeader, String refreshTokenFromCookie, HttpServletResponse response) {
        // Call universal logout to block AT and revoke RT
        logout(authHeader, refreshTokenFromCookie);

        // Expire the HttpOnly cookie for the web client
        if (refreshTokenFromCookie != null) {
            expireRefreshCookie(response);
        }
    }

    // Core logic for Refresh Token Rotation (RTR).
    private AuthenticationResponseDTO rotateRefreshToken(String oldAccessToken, String oldRefreshToken) throws CustomAuthenticationException {

        // Blocklist the Old Access Token
        try {
            Date expiration = jwtService.extractExpiration(oldAccessToken);
            long remainingMillis = expiration.getTime() - System.currentTimeMillis();

            // Blocklist only if there is remaining time
            if (remainingMillis > 0) {
                blocklistService.blockToken(oldAccessToken, Duration.ofMillis(remainingMillis));
                log.info("Blocklisted old AT for immediate invalidation.");
            }
        }  catch (Exception ex) {
            // If the old AT is invalid, we stop the rotation.
            log.warn("Security failure: Cannot process or blocklist old Access Token.", ex);
            throw new CustomAuthenticationException("Invalid access token provided for rotation.");
        }

        // Basic validation
        if (jwtService.isTokenExpired(oldRefreshToken)) {
            throw new CustomAuthenticationException("Refresh token is expired.");
        }

        String username = jwtService.extractUsername(oldRefreshToken);
        var user = userAccountRepository.findByUsername(username).orElseThrow(
                () -> new CustomAuthenticationException("User not found for refresh token.")
        );
        var userDTO = userAccountMapper.userToUserDTO(user);
        String userId = userDTO.getId().toString();

        // Validate against Redis. If stolen/replayed, delete valid token.
        if (!refreshTokenService.isValidForUser(userId, oldRefreshToken)) {
            // Revoke ALL tokens for the user
            refreshTokenService.deleteRefreshToken(userId);
            throw new CustomAuthenticationException("Invalid or revoked refresh token used.");
        }

        // Generate NEW Tokens
        var newAccessToken = jwtService.generateToken(Collections.singletonMap("roles",
                userDTO.getRoles()), userDTO);
        var newRefreshToken = jwtService.generateRefreshToken(userDTO);

        // Update Redis: Blocklist old token and store new one (RTR)
        refreshTokenService.deleteRefreshToken(userId);
        refreshTokenService.storeRefreshToken(userId, newRefreshToken);

        // Return new tokens
        return AuthenticationResponseDTO.builder()
                .jwt(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(userDTO.getUsername())
                .roles(userDTO.getRoles())
                .build();
    }

    // Helper method to create and attach the secure HttpOnly cookie.
    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        long refreshExpirationSec = jwtService.getRefreshExpiration() / 1000;
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true) // Prevents client-side JS access (XSS defense)
                .secure(isSecureCookieTransferEnabled) // Must use HTTPS in prod
                .path("/api/auth") // Only sent to /api/auth endpoints (e.g., /refresh/web)
                .maxAge(refreshExpirationSec)
                .sameSite("Strict") // CSRF defense
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // Helper method to force cookie expiration (for logout).
    private void expireRefreshCookie(HttpServletResponse response) {
        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(0) // Expires immediately
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
    }
}