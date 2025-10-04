package com.gantoniadis.cargopulse.security.service;

import com.gantoniadis.cargopulse.security.dto.AuthenticationRequestDTO;
import com.gantoniadis.cargopulse.security.dto.AuthenticationResponseDTO;
import com.gantoniadis.cargopulse.security.exception.CustomAuthenticationException;
import com.gantoniadis.cargopulse.user.dto.UserAccountDTO;

/**
 * Defines the contract for core authentication and security-related operations.
 */
public interface AuthService {

    /**
     * Authenticates a user using credentials and generates a JWT upon success.
     *
     * @param request The DTO containing the username and password.
     * @return An AuthenticationResponseDTO containing the JWT and user details.
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
     * Extracts the JWT from the Authorization header and adds it to the blocklist (revokes it).
     * Clears the Spring Security context upon processing.
     *
     * @param authHeader The raw value of the Authorization header (e.g., "Bearer <token>").
     */
    void logout(String authHeader);
}
