package com.project.app.domain.authentication.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.domain.authentication.model.AuthenticationRequest;
import com.project.app.domain.authentication.model.AuthenticationResponse;
import com.project.app.domain.authentication.services.AuthenticationService;
import com.project.app.domain.user.model.dto.UserAccountInfo;
import com.project.app.domain.user.services.UserService;
import com.project.app.domain.user.services.UserServiceMapper;

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

    @PostMapping("/account-info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> validate() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccountInfo accountInfo = userMapper.userToUserAccountInfo(userService.getCurrentUser(auth.getName()));
        return ResponseEntity.ok().body(accountInfo);
    }
}
