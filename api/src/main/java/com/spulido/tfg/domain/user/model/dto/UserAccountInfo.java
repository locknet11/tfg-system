package com.spulido.tfg.domain.user.model.dto;

import java.util.List;

import com.spulido.tfg.domain.user.model.ModuleAccess;
import com.spulido.tfg.domain.user.model.Role;

import lombok.Data;

@Data
public class UserAccountInfo {
    String id;
    String email;
    String fullName;
    String company;
    String cuit;
    Role role;
    List<ModuleAccess> moduleAccess;
}
