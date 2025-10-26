package com.spulido.tfg.domain.user.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.shared.ResponseList;
import com.spulido.tfg.domain.user.exception.UserException;
import com.spulido.tfg.domain.user.model.User;
import com.spulido.tfg.domain.user.model.dto.CreateUserRequest;
import com.spulido.tfg.domain.user.model.dto.UpdateUserRequest;
import com.spulido.tfg.domain.user.model.dto.UserAccountInfo;
import com.spulido.tfg.domain.user.model.dto.UsersList;
import com.spulido.tfg.domain.user.services.UserService;
import com.spulido.tfg.domain.user.services.UserServiceMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class UserController {

    private final UserService userService;
    private final UserServiceMapper userMapper;

    @GetMapping()
    public ResponseEntity<ResponseList<UserAccountInfo>> getUsers(
            @RequestParam(name = "params", required = false) Map<String, Object> params,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        UsersList users = userService.listUsers(PageRequest.of(page, size));
        return ResponseEntity.ok().body(userMapper.usersListToResponseList(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccountInfo> getUserById(@PathVariable("id") String id) throws UserException {
        User user = userService.getById(id);
        return ResponseEntity.ok().body(userMapper.userToUserAccountInfo(user));
    }

    @PostMapping()
    public ResponseEntity<?> createUser(@RequestBody @Valid CreateUserRequest request)
            throws URISyntaxException, UserException {
        User created = userService.createUser(userMapper.createUserRequestToUser(request));
        UserAccountInfo dto = userMapper.userToUserAccountInfo(created);
        return ResponseEntity.created(new URI(String.format("/user/%s", dto.getId()))).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccountInfo> updateUserById(@PathVariable("id") String userId,
            @RequestBody @Valid UpdateUserRequest request) throws UserException {
        User updatedUser = userMapper.updateUserRequestToUser(request, userId);
        userService.updateUser(updatedUser);
        UserAccountInfo dto = userMapper.userToUserAccountInfo(updatedUser);
        return ResponseEntity.ok().body(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
