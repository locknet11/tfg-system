package com.spulido.tfg.domain.user.services;

import java.util.List;
import org.springframework.data.domain.PageRequest;

import com.spulido.tfg.domain.user.exception.UserException;
import com.spulido.tfg.domain.user.model.User;
import com.spulido.tfg.domain.user.model.dto.UsersList;

public interface UserService {
    User createUser(User user) throws UserException;

    User updateUser(User user) throws UserException;

    User getById(String id) throws UserException;

    User getByUsername(String username) throws UserException;

    User getCurrentUser(String email);

    List<User> getUsers();

    UsersList listUsers(PageRequest pageRequest);

    void deleteUser(String userId);

    boolean emailExists(String email);
}
