package com.gantoniadis.cargopulse.user.mapper;

import com.gantoniadis.cargopulse.user.dto.RoleDTO;
import com.gantoniadis.cargopulse.user.entity.Role;
import com.gantoniadis.cargopulse.user.util.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "name", source = "name", qualifiedByName = "stringToUserRole")
    Role roleDTOToRole(RoleDTO roleDTO);

    @Mapping(target = "name", source = "name", qualifiedByName = "userRoleToString")
    RoleDTO roleToRoleDTO(Role role);

    Set<Role> roleDTOsSetToRolesSet(Set<RoleDTO> roleDTOs);

    Set<RoleDTO> rolesSetToRoleDTOsSet(Set<Role> roles);

    List<RoleDTO> rolesListToRoleDTOsList(List<Role> roles);

    /** Converts the String name from DTO back to the UserRole enum for the Entity. */
    @Named("stringToUserRole")
    default UserRole stringToUserRole(String name) {
        return UserRole.valueOf(name);
    }

    /** Converts the UserRole enum to String for the DTO. */
    @Named("userRoleToString")
    default String userRoleToString(UserRole name) {
        return name.name();
    }
}