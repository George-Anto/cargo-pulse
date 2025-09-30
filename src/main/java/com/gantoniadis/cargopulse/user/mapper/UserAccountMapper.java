package com.gantoniadis.cargopulse.user.mapper;

import com.gantoniadis.cargopulse.user.dto.UserAccountDTO;
import com.gantoniadis.cargopulse.user.entity.UserAccount;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = RoleMapper.class)
public interface UserAccountMapper {

    UserAccount userDTOToUser(UserAccountDTO userDTO);

    UserAccountDTO userToUserDTO(UserAccount user);

    List<UserAccountDTO> usersListToUserDTOsList(List<UserAccount> roles);
}
