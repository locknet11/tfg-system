package com.spulido.tfg.domain.organization.controller;

import java.net.URI;
import java.net.URISyntaxException;
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
import com.spulido.tfg.domain.organization.model.dto.UpdateOrganizationRequest;
import com.spulido.tfg.domain.organization.services.OrganizationService;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<List<Organization>> getOrganizations() {
        // Since there's only one admin user, return all organizations
        List<Organization> organizations = organizationService.getAllOrganizations();
        return ResponseEntity.ok().body(organizations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganizationById(@PathVariable("id") String id) throws OrganizationException {
        Organization organization = organizationService.getById(id);
        return ResponseEntity.ok().body(organization);
    }

    @PostMapping
    public ResponseEntity<?> createOrganization(@RequestBody @Valid CreateOrganizationRequest request)
            throws URISyntaxException, OrganizationException {
        // Since there's only one admin user, we don't need to track owner/member relationships
        Organization organization = new Organization();
        organization.setName(request.getName())
                   .setDescription(request.getDescription())
                   .setOwnerId("admin"); // Fixed owner ID for the single admin user

        Organization created = organizationService.createOrganization(organization);
        return ResponseEntity.created(new URI(String.format("/api/organizations/%s", created.getId()))).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Organization> updateOrganization(@PathVariable("id") String id,
            @RequestBody @Valid UpdateOrganizationRequest request) throws OrganizationException {
        Organization organization = organizationService.getById(id);
        organization.setName(request.getName())
                   .setDescription(request.getDescription());

        Organization updated = organizationService.updateOrganization(organization);
        return ResponseEntity.ok().body(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrganization(@PathVariable("id") String id) throws OrganizationException {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }
}