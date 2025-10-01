package com.gantoniadis.cargopulse.user.dto;

import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;

@Builder
@Data
public class RoleDTO implements GrantedAuthority {

    @JsonIgnore
    private Long id;

    private String name;

    @Override
    public String getAuthority() {
        return "ROLE_" + name;
    }
}
