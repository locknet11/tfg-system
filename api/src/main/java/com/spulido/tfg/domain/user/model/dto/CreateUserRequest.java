package com.spulido.tfg.domain.user.model.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    @Email
    @NotNull
    String email;

    @Length(min = 8, max = 64)
    @NotNull
    String password;

    @NotNull
    String fullName;
}
