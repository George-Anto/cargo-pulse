package com.gantoniadis.cargopulse.security.dto;

import com.gantoniadis.cargopulse.user.dto.RoleDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

/**
 * Base class for all authentication response DTOs, holding common user information.
 */
@Data
@SuperBuilder // Allows subclasses to inherit the builder pattern
@AllArgsConstructor
@NoArgsConstructor
public abstract class AuthenticationBaseResponseDTO {

    private String username;
    private Set<RoleDTO> roles;
}