package com.gantoniadis.cargopulse.user.service.impl;

import com.gantoniadis.cargopulse.user.dto.AddUserRequestDTO;
import com.gantoniadis.cargopulse.user.dto.RoleDTO;
import com.gantoniadis.cargopulse.user.dto.UserAccountDTO;
import com.gantoniadis.cargopulse.user.entity.Role;
import com.gantoniadis.cargopulse.user.entity.UserAccount;
import com.gantoniadis.cargopulse.user.exception.RequestDataException;
import com.gantoniadis.cargopulse.user.mapper.RoleMapper;
import com.gantoniadis.cargopulse.user.mapper.UserAccountMapper;
import com.gantoniadis.cargopulse.user.repository.RoleRepository;
import com.gantoniadis.cargopulse.user.repository.UserAccountRepository;
import com.gantoniadis.cargopulse.user.service.UserService;
import com.gantoniadis.cargopulse.user.util.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserAccountRepository userAccountRepository;
    private final UserAccountMapper userAccountMapper;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserAccountDTO> getAllUsers() {
        List<UserAccountDTO> allUsersDTO = userAccountMapper.usersListToUserDTOsList(userAccountRepository.findAll());
        log.info("Retrieving all Users: {}", allUsersDTO.stream().map(UserAccountDTO::getUsername).toList());
        return allUsersDTO;
    }

    @Override
    public Optional<UserAccount> findUserByUsername(String username) {
        return userAccountRepository.findByUsername(username);
    }

    @Override
    public UserAccountDTO createNewUser(AddUserRequestDTO request, boolean regularRegister) {

        // 1. Check for existing user
        if (userAccountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RequestDataException("Username '" + request.getUsername() + "' is already taken.");
        }

        List<String> requestedRoles = new ArrayList<>(request.getRoles());

        // 2. Sanitize roles for regular registration
        if (regularRegister) {
            // Remove 'ADMIN' role if included
            requestedRoles.removeIf("ADMIN"::equalsIgnoreCase);

            // Ensure the 'USER' role is present for self-registration
            if (requestedRoles.isEmpty()) {
                requestedRoles.add(UserRole.USER.name());
            }
        }

        // 3. Find and validate roles
        Set<RoleDTO> userRoles = this.addRoles(requestedRoles);

        // 4. Build DTO and encode password
        UserAccountDTO newUserDTO = UserAccountDTO.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(userRoles)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .build();

        // 5. Map, Save, and Return
        UserAccount userToSave = userAccountMapper.userDTOToUser(newUserDTO);
        UserAccount savedUser = userAccountRepository.save(userToSave);
        UserAccountDTO savedUserDTO = userAccountMapper.userToUserDTO(savedUser);

        log.info("New User created: {}", savedUserDTO.getUsername());

        return savedUserDTO;
    }

    @Override
    public long getUserRepositoryCount() {
        return userAccountRepository.count();
    }

    @Override
    public void saveAllRoles(Set<RoleDTO> roleDTOList) {
        roleRepository.saveAll(roleMapper.roleDTOsSetToRolesSet(roleDTOList));
    }

    /**
     * Finds and validates Role entities based on requested String names.
     */
    private Set<RoleDTO> addRoles(List<String> requestRoles) throws RequestDataException {

        // 1. Convert requested role names (Strings) to UserRole enums
        Set<UserRole> roleEnums = requestRoles.stream()
                .map(String::toUpperCase)
                .map(name -> {
                    try {
                        return UserRole.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        // Ignore invalid role strings
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (roleEnums.isEmpty()) {
            throw new RequestDataException("Select at least one valid role for the new User.");
        }

        // 2. Fetch the actual Role entities from the repository
        List<Role> roles = roleRepository.findByNameIn(roleEnums);

        // 3. Final validation (ensures roles actually exist in the DB)
        if (roles.isEmpty()) {
            throw new RequestDataException("No valid roles found for the provided names.");
        }

        // 4. Map entities to DTOs
        return roleMapper.rolesSetToRoleDTOsSet(new HashSet<>(roles));
    }
}