package com.project.app.domain.user.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClientUserRequest extends CreateUserRequest {
    String cuit;
    String company;
}
