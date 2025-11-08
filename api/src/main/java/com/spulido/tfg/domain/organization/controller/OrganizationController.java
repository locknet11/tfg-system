package com.spulido.tfg.domain.organization.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.spulido.tfg.domain.organization.model.dto.AddMemberRequest;
import com.spulido.tfg.domain.organization.model.dto.CreateOrganizationRequest;
import com.spulido.tfg.domain.organization.model.dto.UpdateOrganizationRequest;
import com.spulido.tfg.domain.organization.services.OrganizationService;
import com.spulido.tfg.domain.user.model.User;
import com.spulido.tfg.domain.user.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Organization>> getOrganizations() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getCurrentUser(auth.getName());

        // If admin, return all organizations, otherwise return user's organizations
        List<Organization> organizations;
        if ("ADMIN".equals(currentUser.getRole().toString())) {
            organizations = organizationService.getAllOrganizations();
        } else {
            organizations = organizationService.getOrganizationsByMember(currentUser.getId());
        }

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getCurrentUser(auth.getName());

        Organization organization = new Organization();
        organization.setName(request.getName())
                   .setDescription(request.getDescription())
                   .setOwnerId(currentUser.getId())
                   .setMemberIds(List.of(currentUser.getId()));

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

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(@PathVariable("id") String organizationId,
            @RequestBody AddMemberRequest request) throws OrganizationException {
        organizationService.addMember(organizationId, request.getUserId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable("id") String organizationId,
            @PathVariable("userId") String userId) throws OrganizationException {
        organizationService.removeMember(organizationId, userId);
        return ResponseEntity.noContent().build();
    }
}