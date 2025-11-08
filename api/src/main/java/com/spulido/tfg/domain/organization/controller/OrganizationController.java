package com.spulido.tfg.domain.organization.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.organization.exception.OrganizationException;
import com.spulido.tfg.domain.organization.model.Organization;
import com.spulido.tfg.domain.organization.model.dto.CreateOrganizationRequest;
import com.spulido.tfg.domain.organization.model.dto.OrganizationInfo;
import com.spulido.tfg.domain.organization.model.dto.UpdateOrganizationRequest;
import com.spulido.tfg.domain.organization.services.OrganizationService;
import com.spulido.tfg.domain.organization.services.OrganizationServiceMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationServiceMapper organizationMapper;

    @GetMapping
    public ResponseEntity<List<OrganizationInfo>> getOrganizations() {
        // Since there's only one admin user, return all organizations
        List<Organization> organizations = organizationService.getAllOrganizations();
        List<OrganizationInfo> organizationInfos = organizationMapper.organizationsToOrganizationInfos(organizations);
        return ResponseEntity.ok().body(organizationInfos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationInfo> getOrganizationById(@PathVariable("id") String id) throws OrganizationException {
        Organization organization = organizationService.getById(id);
        OrganizationInfo organizationInfo = organizationMapper.organizationToOrganizationInfo(organization);
        return ResponseEntity.ok().body(organizationInfo);
    }

    @PostMapping
    public ResponseEntity<OrganizationInfo> createOrganization(@RequestBody @Valid CreateOrganizationRequest request)
            throws URISyntaxException, OrganizationException {
        // Since there's only one admin user, we don't need to track owner/member relationships
        Organization organization = organizationMapper.createOrganizationRequestToOrganization(request);
        organization.setOwnerId("admin"); // Fixed owner ID for the single admin user

        Organization created = organizationService.createOrganization(organization);
        OrganizationInfo organizationInfo = organizationMapper.organizationToOrganizationInfo(created);
        return ResponseEntity.created(new URI(String.format("/api/organizations/%s", created.getId()))).body(organizationInfo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationInfo> updateOrganization(@PathVariable("id") String id,
            @RequestBody @Valid UpdateOrganizationRequest request) throws OrganizationException {
        Organization organization = organizationService.getById(id);
        Organization updatedOrganization = organizationMapper.updateOrganizationRequestToOrganization(request, organization);
        Organization updated = organizationService.updateOrganization(updatedOrganization);
        OrganizationInfo organizationInfo = organizationMapper.organizationToOrganizationInfo(updated);
        return ResponseEntity.ok().body(organizationInfo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrganization(@PathVariable("id") String id) throws OrganizationException {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }
}