package com.gantoniadis.cargopulse.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Response DTO for successful Mobile application login.
 * Both tokens (JWT and Refresh Token) are returned directly in the response body.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MobileAuthenticationResponseDTO extends AuthenticationBaseResponseDTO {

    private String jwt;
    private String refreshToken;
}