package com.spulido.tfg.domain.organization.services;

import com.spulido.tfg.domain.organization.model.Organization;
import com.spulido.tfg.domain.organization.model.dto.CreateOrganizationRequest;
import com.spulido.tfg.domain.organization.model.dto.OrganizationInfo;
import com.spulido.tfg.domain.organization.model.dto.UpdateOrganizationRequest;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrganizationServiceMapper {

    private final ModelMapper modelMapper;

    public OrganizationServiceMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Organization createOrganizationRequestToOrganization(CreateOrganizationRequest request) {
        Organization organization = new Organization();
        organization.setName(request.getName())
                   .setDescription(request.getDescription())
                   .setCreatedAt(LocalDateTime.now());
        return organization;
    }

    public Organization updateOrganizationRequestToOrganization(UpdateOrganizationRequest request, Organization organization) {
        organization.setName(request.getName())
                   .setDescription(request.getDescription())
                   .setUpdatedAt(LocalDateTime.now());
        return organization;
    }

    public OrganizationInfo organizationToOrganizationInfo(Organization organization) {
        return modelMapper.map(organization, OrganizationInfo.class);
    }

    public List<OrganizationInfo> organizationsToOrganizationInfos(List<Organization> organizations) {
        return organizations.stream()
                .map(this::organizationToOrganizationInfo)
                .collect(Collectors.toList());
    }
}