package com.spulido.tfg.domain.target.services;

import org.springframework.data.domain.PageRequest;

import com.spulido.tfg.domain.target.exception.TargetException;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.model.dto.TargetsList;

public interface TargetService {
    Target createTarget(Target target) throws TargetException;

    Target updateTarget(Target target) throws TargetException;

    Target getById(String id) throws TargetException;

    TargetsList listTargets(PageRequest pageRequest);

    void deleteTarget(String targetId);

    boolean ipOrDomainExists(String ipOrDomain);
    
    boolean uniqueIdExists(String uniqueId);

    Target getByUniqueId(String uniqueId) throws TargetException;
}