package com.gantoniadis.cargopulse.security.service;

import com.gantoniadis.cargopulse.security.dto.AuthenticationRequestDTO;
import com.gantoniadis.cargopulse.security.dto.AuthenticationResponseDTO;
import com.gantoniadis.cargopulse.user.dto.UserAccountDTO;
import com.gantoniadis.cargopulse.security.exception.CustomAuthenticationException;
import com.gantoniadis.cargopulse.user.mapper.UserAccountMapper;
import com.gantoniadis.cargopulse.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserAccountMapper userAccountMapper;

    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO request) throws CustomAuthenticationException {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new CustomAuthenticationException("User '" + request.getUsername() + "' could not be authenticated");
        }

        var user = userAccountRepository.findByUsername(request.getUsername()).orElseThrow(
                () -> new CustomAuthenticationException("User " + request.getUsername() + " not found in the database")
        );
        var userDTO = userAccountMapper.userToUserDTO(user);
        log.info("Successful authentication of user '{}' with roles: {}", userDTO.getUsername(), userDTO.getRolesNames());

        var jwt = jwtService.generateToken(Collections.singletonMap("roles", userDTO.getRoles()), userDTO);
        return AuthenticationResponseDTO.builder()
                .jwt(jwt)
                .username(userDTO.getUsername())
                .roles(userDTO.getRoles())
                .build();
    }

    public UserAccountDTO getAuthenticatedUser() {
        try {
            String token = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
            String username = jwtService.extractUsername(token);
            return userAccountMapper.userToUserDTO(userAccountRepository.findByUsername(username).
                    orElseThrow(() ->
                            new CustomAuthenticationException("No authorized User found for this action.")));
        } catch (Exception e) {
            throw new CustomAuthenticationException("No authorized User found for this action.");
        }
    }
}