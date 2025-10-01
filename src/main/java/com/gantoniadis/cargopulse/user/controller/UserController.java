package com.gantoniadis.cargopulse.user.controller;

import com.gantoniadis.cargopulse.user.dto.AddUserRequestDTO;
import com.gantoniadis.cargopulse.user.dto.UserAccountDTO;
import com.gantoniadis.cargopulse.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserAccountDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAccountDTO> createNewUser(@RequestBody AddUserRequestDTO request) {
        log.info("Trying to create user with username: {}", request.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createNewUser(request, false));
    }

    @PostMapping("/register")
    public ResponseEntity<UserAccountDTO> registerUser(@RequestBody AddUserRequestDTO request) {
        log.info("Trying to register user with username: {}", request.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createNewUser(request, true));
    }
}
