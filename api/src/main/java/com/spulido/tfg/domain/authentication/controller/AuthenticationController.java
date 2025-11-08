package com.spulido.tfg.domain.authentication.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.authentication.model.AuthenticationRequest;
import com.spulido.tfg.domain.authentication.model.AuthenticationResponse;
import com.spulido.tfg.domain.authentication.services.AuthenticationService;
import com.spulido.tfg.domain.user.model.User;
import com.spulido.tfg.domain.user.model.dto.CreateUserRequest;
import com.spulido.tfg.domain.user.model.dto.UserAccountInfo;
import com.spulido.tfg.domain.user.services.UserService;
import com.spulido.tfg.domain.user.services.UserServiceMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authService;
    private final UserServiceMapper userMapper;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthenticationRequest request) {
        String jwt = authService.singIn(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }

    @PostMapping("/setup")
    public ResponseEntity<?> setup(@RequestBody @Valid CreateUserRequest request) throws Exception {
        // Create the admin user
        User adminUser = userMapper.createUserRequestToUser(request);
        userService.createUser(adminUser);
        // Generate JWT token for the new admin user
        String jwt = authService.singIn(request.getEmail(), request.getPassword());

        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }

    @GetMapping("/check-setup")
    public ResponseEntity<?> checkSetup() {
        List<User> users = userService.getUsers();
        Map<String, Boolean> response = new HashMap<>();
        response.put("needsSetup", users.isEmpty());
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/account-info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> validate() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccountInfo accountInfo = userMapper.userToUserAccountInfo(userService.getCurrentUser(auth.getName()));
        return ResponseEntity.ok().body(accountInfo);
    }
}
