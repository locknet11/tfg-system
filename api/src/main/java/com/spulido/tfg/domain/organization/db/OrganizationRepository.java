package com.spulido.tfg.domain.organization.db;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.spulido.tfg.domain.organization.model.Organization;

public interface OrganizationRepository extends MongoRepository<Organization, String> {
    Optional<Organization> findByName(String name);
    Optional<Organization> findByOrganizationIdentifier(String organizationIdentifier);
    List<Organization> findByOwnerId(String ownerId);

}