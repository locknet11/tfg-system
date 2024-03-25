package com.project.app.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.project.app.domain.user.exception.UserException;
import com.project.app.domain.user.model.User;
import com.project.app.domain.user.model.dto.UserAccountInfo;
import com.project.app.domain.user.services.UserService;
import com.project.app.domain.user.services.UserServiceMapper;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserServiceMapper userMapper;

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") String id) throws UserException {
        User user = userService.getById(id);
        return ResponseEntity.ok().body(user);
    }

    @GetMapping("/my-account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserAccountInfo> getAccountInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccountInfo accountInfo = userMapper.userToUserAccountInfo(userService.getCurrentUser(auth.getName()));
        return ResponseEntity.ok().body(accountInfo);
    }

}
