package com.gantoniadis.cargopulse.user.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@Builder
@Data
@Slf4j
public class UserAccountDTO implements UserDetails {

    private Integer id;
    private String username;
    @JsonIgnore
    private String password;
    private Set<RoleDTO> roles;

    public String getRolesNames() {
        StringBuilder builder = new StringBuilder();
        builder.append("{ ");
        if (roles!= null && !roles.isEmpty()) {
            for (RoleDTO role : roles) {
                builder.append(role.getName());
                builder.append(" ");
            }
        } else {
            builder.append("No roles found ");
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public String getPassword() {
        return password;
    }
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", roles=" + roles +
                '}';
    }
}
