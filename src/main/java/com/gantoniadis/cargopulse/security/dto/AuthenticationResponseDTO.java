package com.gantoniadis.cargopulse.security.dto;

import com.gantoniadis.cargopulse.user.dto.RoleDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponseDTO {

    private String jwt;
    private String username;
    private Set<RoleDTO> roles;
}