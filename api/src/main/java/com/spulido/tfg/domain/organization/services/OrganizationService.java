package com.spulido.tfg.domain.organization.services;

import java.util.List;

import com.spulido.tfg.domain.organization.exception.OrganizationException;
import com.spulido.tfg.domain.organization.model.Organization;

public interface OrganizationService {
    Organization createOrganization(Organization organization) throws OrganizationException;

    Organization updateOrganization(Organization organization) throws OrganizationException;

    Organization getById(String id) throws OrganizationException;

    Organization getByName(String name) throws OrganizationException;

    List<Organization> getOrganizationsByOwner(String ownerId);

    List<Organization> getOrganizationsByMember(String memberId);

    List<Organization> getAllOrganizations();

    void deleteOrganization(String organizationId) throws OrganizationException;

    boolean organizationNameExists(String name);

    void addMember(String organizationId, String userId) throws OrganizationException;

    void removeMember(String organizationId, String userId) throws OrganizationException;

    void addProject(String organizationId, String projectId) throws OrganizationException;
}