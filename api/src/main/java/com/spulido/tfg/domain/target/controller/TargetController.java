package com.spulido.tfg.domain.target.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.shared.ResponseList;
import com.spulido.tfg.domain.target.exception.TargetException;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.model.dto.CreateTargetRequest;
import com.spulido.tfg.domain.target.model.dto.TargetInfo;
import com.spulido.tfg.domain.target.model.dto.UpdateTargetRequest;
import com.spulido.tfg.domain.target.services.TargetService;
import com.spulido.tfg.domain.target.services.TargetServiceMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TargetController {

    private final TargetService targetService;
    private final TargetServiceMapper targetMapper;

    @GetMapping()
    public ResponseEntity<ResponseList<TargetInfo>> getTargets(
            @RequestParam(name = "params", required = false) Map<String, Object> params,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        var targets = targetService.listTargets(PageRequest.of(page, size));
        return ResponseEntity.ok().body(targetMapper.targetsListToResponseList(targets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TargetInfo> getTargetById(@PathVariable("id") String id) throws TargetException {
        Target target = targetService.getById(id);
        return ResponseEntity.ok().body(targetMapper.targetToTargetInfo(target));
    }

    @PostMapping()
    public ResponseEntity<?> createTarget(@RequestBody @Valid CreateTargetRequest request)
            throws URISyntaxException, TargetException {
        Target created = targetService.createTarget(targetMapper.createTargetRequestToTarget(request));
        TargetInfo dto = targetMapper.targetToTargetInfo(created);
        return ResponseEntity.created(new URI(String.format("/api/targets/%s", dto.getId()))).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TargetInfo> updateTargetById(@PathVariable("id") String targetId,
            @RequestBody @Valid UpdateTargetRequest request) throws TargetException {
        Target updatedTarget = targetMapper.updateTargetRequestToTarget(request, targetId);
        targetService.updateTarget(updatedTarget);
        TargetInfo dto = targetMapper.targetToTargetInfo(updatedTarget);
        return ResponseEntity.ok().body(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTarget(@PathVariable("id") String id) {
        targetService.deleteTarget(id);
        return ResponseEntity.noContent().build();
    }

}