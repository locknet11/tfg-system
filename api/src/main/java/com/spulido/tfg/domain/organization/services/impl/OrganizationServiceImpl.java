package com.spulido.tfg.domain.organization.services.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

import com.spulido.tfg.common.util.IdentifierGenerator;
import com.spulido.tfg.domain.organization.db.OrganizationRepository;
import com.spulido.tfg.domain.organization.exception.OrganizationException;
import com.spulido.tfg.domain.organization.model.Organization;
import com.spulido.tfg.domain.organization.services.OrganizationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository repository;

    @Override
    public Organization createOrganization(Organization organization) throws OrganizationException {
        if (organizationNameExists(organization.getName())) {
            throw new OrganizationException("organization.name.alreadyExists");
        }
        
        // Generate unique organization identifier
        String identifier;
        do {
            identifier = IdentifierGenerator.generateOrganizationIdentifier();
        } while (repository.findByOrganizationIdentifier(identifier).isPresent());
        
        organization.setOrganizationIdentifier(identifier);
        return repository.save(organization);
    }

    @Override
    public Organization updateOrganization(Organization organization) throws OrganizationException {
        if (!repository.existsById(organization.getId())) {
            throw new OrganizationException("organization.error.notfound");
        }
        return repository.save(organization);
    }

    @Override
    public Organization getById(String id) throws OrganizationException {
        return repository.findById(id).orElseThrow(() -> new OrganizationException("organization.error.notfound"));
    }

    @Override
    public Organization getByName(String name) throws OrganizationException {
        return repository.findByName(name).orElseThrow(() -> new OrganizationException("organization.error.notfound"));
    }

    @Override
    public List<Organization> getOrganizationsByOwner(String ownerId) {
        return repository.findByOwnerId(ownerId);
    }

    @Override
    public List<Organization> getAllOrganizations() {
        return repository.findAll();
    }

    @Override
    public void deleteOrganization(String organizationId) throws OrganizationException {
        if (!repository.existsById(organizationId)) {
            throw new OrganizationException("organization.error.notfound");
        }
        repository.deleteById(organizationId);
    }

    @Override
    public boolean organizationNameExists(String name) {
        Optional<Organization> orgOptional = repository.findByName(name);
        return orgOptional.isPresent();
    }

    @Override
    public void addProject(String organizationId, String projectId) throws OrganizationException {
        Organization organization = getById(organizationId);
        if (!organization.getProjectIds().contains(projectId)) {
            organization.getProjectIds().add(projectId);
            repository.save(organization);
        }
    }

    @Override
    public Organization getByOrganizationIdentifier(String organizationIdentifier) throws OrganizationException {
        return repository.findByOrganizationIdentifier(organizationIdentifier)
                .orElseThrow(() -> new OrganizationException("organization.error.notfound"));
    }
}