package com.gantoniadis.cargopulse.security.service;

import com.gantoniadis.cargopulse.security.dto.AuthenticationRequestDTO;
import com.gantoniadis.cargopulse.security.dto.AuthenticationResponseDTO;
import com.gantoniadis.cargopulse.security.exception.CustomAuthenticationException;
import com.gantoniadis.cargopulse.user.dto.UserAccountDTO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Defines the contract for core authentication and security-related operations.
 * This service handles client-specific token generation (Web vs. Mobile) and Refresh Token Rotation (RTR).
 */
public interface AuthService {

    /**
     * Authenticates a user using credentials and generates a JWT (Access Token) and a Refresh Token upon success.
     * This is the core logic used by both web and mobile login flows.
     *
     * @param request The DTO containing the username and password.
     * @return An AuthenticationResponseDTO containing the Access Token, Refresh Token, and user details.
     * @throws CustomAuthenticationException If authentication fails (bad credentials, user not found).
     */
    AuthenticationResponseDTO authenticate(AuthenticationRequestDTO request) throws CustomAuthenticationException;

    /**
     * Retrieves the details of the currently authenticated user from the SecurityContext.
     *
     * @return The UserAccountDTO of the authenticated user.
     * @throws CustomAuthenticationException If no user is found in the context or token is invalid.
     */
    UserAccountDTO getAuthenticatedUser() throws CustomAuthenticationException;

    /**
     * Performs a universal logout: blocks the Access Token and revokes the Refresh Token from storage (e.g., Redis).
     * Clears the Spring Security context upon processing.
     *
     * @param authHeader The raw value of the Authorization header (e.g., "Bearer <token>").
     * @param refreshToken The Refresh Token string to be revoked from storage.
     */
    void logout(String authHeader, String refreshToken);

    /**
     * Handles the login process specifically for **Web Browser** clients.
     * Calls the core authentication logic, sets the Refresh Token in a secure {@code HttpOnly} cookie,
     * and removes the Refresh Token from the response body.
     *
     * @param request The authentication request DTO.
     * @param response The HttpServletResponse to attach the {@code HttpOnly} cookie to.
     * @return An AuthenticationResponseDTO containing only the Access Token (JWT) in the body.
     */
    AuthenticationResponseDTO authenticateWeb(AuthenticationRequestDTO request, HttpServletResponse response);

    /**
     * Handles the token refresh process specifically for **Web Browser** clients.
     * Implements Refresh Token Rotation (RTR): validates the old token (from cookie),
     * generates a new Access Token and Refresh Token, blocks the old RT, and sets the new RT in an {@code HttpOnly} cookie.
     *
     * @param oldRefreshToken The Refresh Token extracted from the incoming {@code HttpOnly} cookie.
     * @param response The HttpServletResponse to attach the **new** {@code HttpOnly} cookie to.
     * @return An AuthenticationResponseDTO containing only the new Access Token (JWT) in the body.
     */
    AuthenticationResponseDTO refreshWeb(String oldAccessToken, String oldRefreshToken, HttpServletResponse response);

    /**
     * Handles the login process specifically for **Mobile App** clients.
     * Calls the core authentication logic and returns both the Access Token and the Refresh Token in the response body.
     *
     * @param request The authentication request DTO.
     * @return An AuthenticationResponseDTO containing both the Access Token and the Refresh Token in the body.
     */
    AuthenticationResponseDTO authenticateMobile(AuthenticationRequestDTO request);

    /**
     * Handles the token refresh process specifically for **Mobile App** clients.
     * Implements Refresh Token Rotation (RTR): validates the old token (from header/body),
     * generates a new Access Token and Refresh Token, and blocks the old RT.
     *
     * @param oldRefreshToken The Refresh Token extracted from the client's custom header or body.
     * @return An AuthenticationResponseDTO containing both the new Access Token and the new Refresh Token in the body.
     */
    AuthenticationResponseDTO refreshMobile(String oldAccessToken, String oldRefreshToken);

    /**
     * Handles the logout process specifically for **Web Browser** clients.
     * Calls the universal {@code logout} logic and then expires the Refresh Token {@code HttpOnly} cookie.
     *
     * @param authHeader The raw value of the Authorization header (for blocking the AT).
     * @param refreshTokenFromCookie The Refresh Token extracted from the cookie (to be revoked).
     * @param response The HttpServletResponse to set the expired cookie on.
     */
    void logoutWeb(String authHeader, String refreshTokenFromCookie, HttpServletResponse response);
}