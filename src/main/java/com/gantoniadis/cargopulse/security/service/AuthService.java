package com.gantoniadis.cargopulse.security.service;

import com.gantoniadis.cargopulse.security.dto.AuthenticationRequestDTO;
import com.gantoniadis.cargopulse.security.dto.MobileAuthenticationResponseDTO;
import com.gantoniadis.cargopulse.security.dto.WebAuthenticationResponseDTO;
import com.gantoniadis.cargopulse.security.exception.CustomAuthenticationException;
import com.gantoniadis.cargopulse.user.dto.UserAccountDTO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Defines the contract for core authentication and security-related operations.
 * This service handles client-specific token generation (Web vs. Mobile) and Refresh Token Rotation (RTR).
 */
public interface AuthService {

    /**
     * Retrieves the details of the currently authenticated user from the SecurityContext.
     *
     * @return The UserAccountDTO of the authenticated user.
     * @throws CustomAuthenticationException If no user is found in the context or token is invalid.
     */
    UserAccountDTO getAuthenticatedUser() throws CustomAuthenticationException;

    /**
     * Handles the login process specifically for **Web Browser** clients.
     * Calls the core authentication logic, sets the Access and Refresh Tokens in secure {@code HttpOnly} cookies.
     *
     * @param request The authentication request DTO.
     * @param response The HttpServletResponse to attach the {@code HttpOnly} cookie to.
     * @return An WebAuthenticationResponseDTO containing only username and roles.
     */
    WebAuthenticationResponseDTO authenticateWeb(AuthenticationRequestDTO request, HttpServletResponse response);

    /**
     * Handles the login process specifically for **Mobile App** clients.
     * Calls the core authentication logic and returns both the Access Token and the Refresh Token in the response body.
     *
     * @param request The authentication request DTO.
     * @return An MobileAuthenticationResponseDTO containing both the Access Token and the Refresh Token in the body.
     */
    MobileAuthenticationResponseDTO authenticateMobile(AuthenticationRequestDTO request);

    /**
     * Handles the token refresh process specifically for **Web Browser** clients.
     * Implements Refresh Token Rotation (RTR): validates the old token (from cookie),
     * generates a new Access Token and Refresh Token, blocks the old RT, and sets the new At and RT in an {@code HttpOnly} cookie.
     *
     * @param oldRefreshToken The Refresh Token extracted from the incoming {@code HttpOnly} cookie.
     * @param response The HttpServletResponse to attach the **new** {@code HttpOnly} cookie to.
     * @return An WebAuthenticationResponseDTO containing only username and roles in the body.
     */
    WebAuthenticationResponseDTO refreshWeb(String oldAccessToken, String oldRefreshToken, HttpServletResponse response);

    /**
     * Handles the token refresh process specifically for **Mobile App** clients.
     * Implements Refresh Token Rotation (RTR): validates the old token (from header/body),
     * generates a new Access Token and Refresh Token, and blocks the old RT.
     *
     * @param oldRefreshToken The Refresh Token extracted from the client's custom header or body.
     * @return An MobileAuthenticationResponseDTO containing both the new Access Token and the new Refresh Token in the body.
     */
    MobileAuthenticationResponseDTO refreshMobile(String oldAccessToken, String oldRefreshToken);

    /**
     * Handles logout for **Web Browser** clients.
     * This operation blocks the short-lived Access Token (AT) and revokes the Refresh Token (RT) from Redis.
     * Crucially, it forces the expiration of both the AT and RT {@code HttpOnly} cookies in the browser.
     *
     * @param accessToken The raw Access Token value extracted from the incoming {@code accessToken} cookie (may be null/expired).
     * @param refreshTokenFromCookie The raw Refresh Token value extracted from the incoming {@code refreshToken} cookie (may be null/expired).
     * @param response The HttpServletResponse used to set the zero-max-age cookies (expire the session).
     */
    void logoutWeb(String accessToken, String refreshTokenFromCookie, HttpServletResponse response);

    /**
     * Handles logout for **Mobile** clients. This method requires the Refresh Token for session revocation.
     *
     * @param authHeader The full {@code Authorization: Bearer <AT>} header value (may be null/empty if client clears it). Used to blocklist the AT.
     * @param refreshToken The raw Refresh Token from the request body. This is **required** to identify and revoke the session in Redis.
     */
    void logoutMobile(String authHeader, String refreshToken);
}