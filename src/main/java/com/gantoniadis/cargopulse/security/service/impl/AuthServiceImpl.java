package com.gantoniadis.cargopulse.security.service.impl;

import com.gantoniadis.cargopulse.config.properties.SecurityProperties;
import com.gantoniadis.cargopulse.security.dto.AuthenticationRequestDTO;
import com.gantoniadis.cargopulse.security.dto.MobileAuthenticationResponseDTO;
import com.gantoniadis.cargopulse.security.dto.WebAuthenticationResponseDTO;
import com.gantoniadis.cargopulse.security.exception.CustomAuthenticationException;
import com.gantoniadis.cargopulse.security.service.AuthService;
import com.gantoniadis.cargopulse.security.service.JwtBlocklistService;
import com.gantoniadis.cargopulse.security.service.JwtService;
import com.gantoniadis.cargopulse.security.service.RefreshTokenService;
import com.gantoniadis.cargopulse.security.util.TokenExtractionHelper;
import com.gantoniadis.cargopulse.user.dto.UserAccountDTO;
import com.gantoniadis.cargopulse.user.mapper.UserAccountMapper;
import com.gantoniadis.cargopulse.user.repository.UserAccountRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Service handling core authentication, token generation, and Refresh Token Rotation (RTR) logic.
 * * NOTE ON SECURITY (Token Replay Defense):
 * ---------------------------------------
 * In the rotateRefreshToken() method, if a blocklisted (old) Access Token is detected,
 * the service immediately revokes the associated valid Refresh Token (RT) from Redis.
 * This terminates the session's renewal capability, defending against token replay attacks.
 * * The short-lived Access Token (AT) that the legitimate client currently holds remains valid
 * until its natural expiration (e.g., 15 minutes). This is an accepted risk trade-off in
 * stateless JWT systems, as forcing full AT revocation is complex and costly. The primary
 * defense is the *immediate revocation of the long-lived RT*.
 */
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

    private final SecurityProperties securityProperties;

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

    // WEB Login: Calls core logic, sets HttpOnly cookie, and cleans DTO for response body.
    @Override
    public WebAuthenticationResponseDTO authenticateWeb(AuthenticationRequestDTO request,
                                                           HttpServletResponse response) {
        // Get tokens from core logic
        var unfilteredResponseDTO = authenticate(request);

        // Set the Access and Refresh Tokens as a secure HttpOnly cookie (XSS defense)
        setAccessCookie(response, unfilteredResponseDTO.getJwt());
        setRefreshCookie(response, unfilteredResponseDTO.getRefreshToken());

        // Create the web response dto
        return WebAuthenticationResponseDTO
                .builder()
                .username(unfilteredResponseDTO.getUsername())
                .roles(unfilteredResponseDTO.getRoles())
                .build();
    }

    // MOBILE Login: Calls the core logic and returns the DTO with both tokens.
    @Override
    public MobileAuthenticationResponseDTO authenticateMobile(AuthenticationRequestDTO request) {
        return authenticate(request);
    }

    // WEB Refresh: Calls rotation logic, sets NEW HttpOnly cookie, and cleans DTO.
    @Override
    public WebAuthenticationResponseDTO refreshWeb(String oldAccessToken, String oldRefreshToken, HttpServletResponse response) {
        // Core rotation logic (generates new AT/RT, blocks old RT in Redis)
        var unfilteredResponseDTO = rotateRefreshToken(oldAccessToken, oldRefreshToken, false);

        // Set the NEW Access and Refresh Tokens as a secure HttpOnly cookie
        setAccessCookie(response, unfilteredResponseDTO.getJwt());
        setRefreshCookie(response, unfilteredResponseDTO.getRefreshToken());

        // Create the web response dto
        return WebAuthenticationResponseDTO
                .builder()
                .username(unfilteredResponseDTO.getUsername())
                .roles(unfilteredResponseDTO.getRoles())
                .build();
    }

    // MOBILE Refresh: Calls the rotation logic. RT remains in the DTO body for mobile client.
    @Override
    public MobileAuthenticationResponseDTO refreshMobile(String oldAccessToken, String oldRefreshToken) {
        return rotateRefreshToken(oldAccessToken, oldRefreshToken, true);
    }

    // LOGOUT WEB: Distinct method for web logic (AT/RT in Cookies)
    @Override
    public void logoutWeb(String accessToken, String refreshTokenFromCookie, HttpServletResponse response) {

        // accessToken and refreshTokenFromCookie may be null if cookies expired/missing

        // 1. Block AT if present
        if (accessToken != null) {
            try {
                if (!jwtService.isTokenExpired(accessToken)) {
                    Date expiration = jwtService.extractExpiration(accessToken);
                    long remainingMillis = expiration.getTime() - System.currentTimeMillis();
                    if (remainingMillis > 0) {
                        blocklistService.blockToken(accessToken, Duration.ofMillis(remainingMillis));
                        log.info("Web AT blocklisted for revocation.");
                    }
                } else {
                    log.info("Skipping blocklisting of Access Token, for web logout, as it is already expired.");
                }
            } catch (ExpiredJwtException ex) {
                log.info("Access Token expired during blocklist processing, for web logout, skipping blocklist and continuing rotation.");
            } catch (Exception ex) {
                log.warn("Error processing Web Access Token during logout: {}", ex.getMessage());
            }
        }

        // 2. Revoke RT if present
        if (refreshTokenFromCookie != null) {
            try {
                // Delete RT from Redis to instantly revoke session access
                String username = jwtService.extractUsername(refreshTokenFromCookie);
                var user = userAccountRepository.findByUsername(username);
                if (user.isPresent()) {
                    String userId = user.get().getId().toString();
                    refreshTokenService.deleteRefreshToken(userId);
                    log.info("Web Refresh Token revoked successfully for user {}.", username);
                }
            } catch (Exception ex) {
                log.warn("Error processing Web Refresh Token during logout: {}", ex.getMessage());
            }
        }

        // 3. Expire the HttpOnly cookies for the web client (always do this for a clean state)
        expireAccessCookie(response);
        expireRefreshCookie(response);

        // 4. Clear the Security Context
        SecurityContextHolder.clearContext();
    }

    // LOGOUT MOBILE: Distinct method for mobile logic (AT in Header, RT in Body)
    @Override
    public void logoutMobile(String authHeader, String refreshToken) {

        String accessToken = TokenExtractionHelper.extractAccessTokenFromHeader(authHeader);

        if (accessToken == null) {
            throw new CustomAuthenticationException("Authorization header must contain a 'Bearer' Access Token for mobile logout.");
        }

        // 1. Block AT
        try {
            if (!jwtService.isTokenExpired(accessToken)) {
                Date expiration = jwtService.extractExpiration(accessToken);
                long remainingMillis = expiration.getTime() - System.currentTimeMillis();
                if (remainingMillis > 0) {
                    blocklistService.blockToken(accessToken, Duration.ofMillis(remainingMillis));
                    log.info("Mobile AT blocklisted for revocation.");
                }
            } else {
                log.info("Skipping blocklisting of Access Token, for mobile logout, as it is already expired.");
            }
        } catch (ExpiredJwtException ex) {
            log.info("Access Token expired during blocklist processing, for mobile logout, skipping blocklist and continuing rotation.");
        } catch (Exception ex) {
            log.warn("Error processing Mobile Access Token during logout: {}", ex.getMessage());
        }

        // 2. Revoke RT
        try {
            // Delete RT from Redis to instantly revoke session access
            String username = jwtService.extractUsername(refreshToken);
            var user = userAccountRepository.findByUsername(username);
            if (user.isPresent()) {
                String userId = user.get().getId().toString();
                refreshTokenService.deleteRefreshToken(userId);
                log.info("Mobile Refresh Token revoked successfully for user {}.", username);
            } else {
                log.warn("Mobile RT could not be revoked: User not found for RT username.");
            }
        } catch (Exception ex) {
            log.warn("Error processing Mobile Refresh Token during logout: {}", ex.getMessage());
        }

        // 3. Clear the Security Context
        SecurityContextHolder.clearContext();
    }

    // Core authentication logic: authenticates user, generates AT and RT, stores RT in Redis.
    private MobileAuthenticationResponseDTO authenticate(AuthenticationRequestDTO request) throws CustomAuthenticationException {
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
        return MobileAuthenticationResponseDTO.builder()
                .jwt(accessToken)
                .refreshToken(refreshToken)
                .username(userDTO.getUsername())
                .roles(userDTO.getRoles())
                .build();
    }

    // Core logic for Refresh Token Rotation (RTR).
    private MobileAuthenticationResponseDTO rotateRefreshToken(String oldAccessToken, String oldRefreshToken, boolean isMobileRequest)
            throws CustomAuthenticationException {

        // For web requests we can have null old AT because when it expires,
        // the cookie is deleted too by the browser and is not sent with the request

        // 1. Check for Token Replay (Blocklist Check)
        if ((isMobileRequest || oldAccessToken != null)
                && blocklistService.isTokenBlocklisted(oldAccessToken)) {

            try {
                UserAccountDTO userDTO = getUserFromRefreshToken(oldRefreshToken);

                // Revoke all tokens for the user and force re-login (Replay defense)
                String userId = userDTO.getId().toString();
                refreshTokenService.deleteRefreshToken(userId);
                log.warn("Security failure: Blocklisted AT used in refresh request. Revoking RT token for user: {}", userDTO.getUsername());
            } catch (CustomAuthenticationException ex) {
                log.warn("Security failure: Blocklisted AT used, but RT was invalid/expired or user not found. Exception: {}", ex.getMessage());
            }

            throw new CustomAuthenticationException("Invalid or previously used access token detected for rotation.");
        }

        // 2. Blocklist the Old Access Token
        if (oldAccessToken != null) {
            try {
                if (!jwtService.isTokenExpired(oldAccessToken)) {
                    // If it's still valid, we extract expiration to calculate remaining time
                    // This call might still throw an ExpiredJwtException in a rare race condition,
                    // but the catch block below will handle it gracefully.
                    Date expiration = jwtService.extractExpiration(oldAccessToken);
                    long remainingMillis = expiration.getTime() - System.currentTimeMillis();
                    if (remainingMillis > 0) {
                        blocklistService.blockToken(oldAccessToken, Duration.ofMillis(remainingMillis));
                        log.info("Blocklisted old AT for immediate invalidation.");
                    }
                } else {
                    log.info("Skipping blocklisting of old Access Token as it is already expired.");
                }
            } catch (ExpiredJwtException ex) {
                log.info("Old Access Token expired during blocklist processing, skipping blocklist and continuing rotation.");
            } catch (Exception ex) {
                log.warn("Security failure: Cannot process or blocklist old Access Token.", ex);
                throw new CustomAuthenticationException("Invalid access token provided for rotation.");
            }
        }

        // 3. Centralized validation and user retrieval
        UserAccountDTO userDTO = getUserFromRefreshToken(oldRefreshToken);
        String userId = userDTO.getId().toString();

        // 4. Validate against Redis. If stolen/replayed, delete valid token.
        if (!refreshTokenService.isValidForUser(userId, oldRefreshToken)) {
            // Revoke all tokens for the user
            refreshTokenService.deleteRefreshToken(userId);
            throw new CustomAuthenticationException("Invalid or revoked refresh token used.");
        }

        // 5. Generate new Tokens
        var newAccessToken = jwtService.generateToken(Collections.singletonMap("roles",
                userDTO.getRoles()), userDTO);
        var newRefreshToken = jwtService.generateRefreshToken(userDTO);

        // 6. Update Redis: Blocklist old token and store new one (RTR)
        refreshTokenService.deleteRefreshToken(userId);
        refreshTokenService.storeRefreshToken(userId, newRefreshToken);

        // 7. Return new tokens
        return MobileAuthenticationResponseDTO.builder()
                .jwt(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(userDTO.getUsername())
                .roles(userDTO.getRoles())
                .build();
    }

    /**
     * Extracts username from the Refresh Token, finds the corresponding User,
     * and handles exceptions.
     * @param refreshToken The token to process.
     * @return The DTO of the authenticated user.
     * @throws CustomAuthenticationException if the token is expired or the user is not found.
     */
    private UserAccountDTO getUserFromRefreshToken(String refreshToken) throws CustomAuthenticationException {
        if (jwtService.isTokenExpired(refreshToken)) {
            throw new CustomAuthenticationException("Refresh token is expired.");
        }

        String username = jwtService.extractUsername(refreshToken);
        var user = userAccountRepository.findByUsername(username).orElseThrow(
                () -> new CustomAuthenticationException("User not found for refresh token.")
        );

        return userAccountMapper.userToUserDTO(user);
    }

    // Helper method to create and attach the secure HttpOnly Access Token cookie.
    private void setAccessCookie(HttpServletResponse response, String accessToken) {
        long accessExpirationSec = jwtService.getExpiration() / 1000;
        ResponseCookie cookie = ResponseCookie.from("accessToken", accessToken) // Changed name to 'accessToken'
                .httpOnly(true) // Prevents client-side JS access (XSS defense)
                .secure(securityProperties.getCookie().isSslTransfer()) // Must use HTTPS in prod
                .path("/") // Path set to root (/) for ALL API endpoints
                .maxAge(accessExpirationSec)
                .sameSite("None") // The future frontend will be on another domain
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // Helper method to force cookie expiration (for logout).
    private void expireAccessCookie(HttpServletResponse response) {
        ResponseCookie expiredCookie = ResponseCookie.from("accessToken", "") // Changed name
                .httpOnly(true)
                .secure(securityProperties.getCookie().isSslTransfer())
                .path("/") // Path set to root (/)
                .maxAge(0) // Expires immediately
                .sameSite("None") // The future frontend will be on another domain
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
    }

    // Helper method to create and attach the secure HttpOnly cookie.
    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        long refreshExpirationSec = jwtService.getRefreshExpiration() / 1000;
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true) // Prevents client-side JS access (XSS defense)
                .secure(securityProperties.getCookie().isSslTransfer()) // Must use HTTPS in prod
                .path("/api/auth") // Only sent to /api/auth endpoints (e.g., /refresh/web)
                .maxAge(refreshExpirationSec)
                .sameSite("None") // The future frontend will be on another domain
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // Helper method to force cookie expiration (for logout).
    private void expireRefreshCookie(HttpServletResponse response) {
        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(securityProperties.getCookie().isSslTransfer())
                .path("/api/auth")
                .maxAge(0) // Expires immediately
                .sameSite("None") // The future frontend will be on another domain
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
    }
}
