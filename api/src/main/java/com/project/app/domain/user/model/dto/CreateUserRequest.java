package com.project.app.domain.user.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    String email;
    String password;
    String fullName;
}
