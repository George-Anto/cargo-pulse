package com.gantoniadis.cargopulse.security.dto;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Response DTO for successful Web browser login.
 * Tokens (JWT, Refresh Token) are sent via HttpOnly cookies and are excluded from the body.
 */
@SuperBuilder
@NoArgsConstructor
public class WebAuthenticationResponseDTO extends AuthenticationBaseResponseDTO {
}
