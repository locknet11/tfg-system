package com.spulido.tfg.domain.target.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.spulido.tfg.common.util.IdentifierGenerator;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.target.exception.TargetException;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.model.TargetStatus;
import com.spulido.tfg.domain.target.model.dto.TargetsList;
import com.spulido.tfg.domain.target.services.TargetService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TargetServiceImpl implements TargetService {

    private final TargetRepository repository;

    @Override
    public Target createTarget(Target target) throws TargetException {
        // Generate unique 5-character alphanumeric ID for agent setup URL
        String uniqueId;
        do {
            uniqueId = IdentifierGenerator.generateTargetUniqueId();
        } while (uniqueIdExists(uniqueId));
        
        target.setUniqueId(uniqueId);
        
        // Set default values for fields that will be populated later by agent
        target.setStatus(TargetStatus.OFFLINE);
        target.setAssignedAgent(null);
        target.setIpOrDomain(null);
        
        return repository.save(target);
    }

    @Override
    public Target updateTarget(Target target) throws TargetException {
        Target existing = getById(target.getId());
        if (target.getIpOrDomain() != null && !target.getIpOrDomain().equals(existing.getIpOrDomain()) 
            && ipOrDomainExists(target.getIpOrDomain())) {
            throw new TargetException("target.error.ipOrDomainExists");
        }
        return repository.save(target);
    }

    @Override
    public Target getById(String id) throws TargetException {
        return repository.findByIdScoped(id).orElseThrow(() -> new TargetException("target.error.notfound"));
    }

    @Override
    public TargetsList listTargets(PageRequest pageRequest) {
        Page<Target> page = repository.findAllScoped(pageRequest);
        return new TargetsList(page.getContent(), pageRequest, page.getTotalElements());
    }

    @Override
    public void deleteTarget(String targetId) {
        // Delete using scoped query to ensure user can only delete their own targets
        repository.findByIdScoped(targetId).ifPresent(repository::delete);
    }

    @Override
    public boolean ipOrDomainExists(String ipOrDomain) {
        if (ipOrDomain == null || ipOrDomain.isEmpty()) {
            return false;
        }
        return repository.findByIpOrDomain(ipOrDomain).isPresent();
    }

    @Override
    public boolean uniqueIdExists(String uniqueId) {
        return repository.findByUniqueId(uniqueId).isPresent();
    }

    @Override
    public Target getByUniqueId(String uniqueId) throws TargetException {
        return repository.findByUniqueId(uniqueId)
                .orElseThrow(() -> new TargetException("target.error.notfound"));
    }

}