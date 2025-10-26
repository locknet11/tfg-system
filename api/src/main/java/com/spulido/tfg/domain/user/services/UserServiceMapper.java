package com.spulido.tfg.domain.user.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.spulido.tfg.domain.shared.ListMapper;
import com.spulido.tfg.domain.shared.ResponseList;
import com.spulido.tfg.domain.user.exception.UserException;
import com.spulido.tfg.domain.user.model.Role;
import com.spulido.tfg.domain.user.model.User;
import com.spulido.tfg.domain.user.model.dto.CreateUserRequest;
import com.spulido.tfg.domain.user.model.dto.UpdateUserRequest;
import com.spulido.tfg.domain.user.model.dto.UserAccountInfo;
import com.spulido.tfg.domain.user.model.dto.UsersList;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UserServiceMapper {

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final ModelMapper modelMapper;

    public User createUserRequestToUser(CreateUserRequest request) throws UserException {
        if (emailExists(request.getEmail())) {
            throw new UserException("user.email.alreadyExists");
        }
        User user = new User();
        user.setEmail(request.getEmail())
                .setFullName(request.getFullName())
                .setEncodedPassword(passwordEncoder.encode(request.getPassword()))
                .setRole(Role.USER)
                .setCreatedAt(LocalDateTime.now());
        return user;
    }

    public UserAccountInfo userToUserAccountInfo(User user) {
        return modelMapper.map(user, UserAccountInfo.class);
    }

    public ResponseList<UserAccountInfo> usersListToResponseList(UsersList list) {
        List<UserAccountInfo> accountInfoList = userListToUserAccountInfoList(list.getContent());
        PageImpl<UserAccountInfo> dtoPage = new PageImpl<>(accountInfoList, list.getPageable(),
                list.getTotalElements());
        return ListMapper.mapList(dtoPage);
    }

    public User updateUserRequestToUser(UpdateUserRequest request, String userId) throws UserException {
        User user = userService.getById(userId);
        user.setEmail(request.getEmail())
                .setFullName(request.getFullName())
                .setModuleAccess(request.getModuleAccess())
                .setRole(request.getRole())
                .setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private List<UserAccountInfo> userListToUserAccountInfoList(List<User> users) {
        return users
                .stream()
                .map(user -> modelMapper.map(user, UserAccountInfo.class))
                .collect(Collectors.toList());
    }

    private boolean emailExists(String email) {
        return userService.emailExists(email);
    }
}
