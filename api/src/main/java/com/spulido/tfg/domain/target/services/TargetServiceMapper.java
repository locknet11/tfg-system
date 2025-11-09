package com.spulido.tfg.domain.target.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import com.spulido.tfg.domain.shared.ListMapper;
import com.spulido.tfg.domain.shared.ResponseList;
import com.spulido.tfg.domain.target.exception.TargetException;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.model.dto.CreateTargetRequest;
import com.spulido.tfg.domain.target.model.dto.TargetInfo;
import com.spulido.tfg.domain.target.model.dto.TargetsList;
import com.spulido.tfg.domain.target.model.dto.UpdateTargetRequest;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class TargetServiceMapper {

    private final TargetService targetService;
    private final ModelMapper modelMapper;

    public Target createTargetRequestToTarget(CreateTargetRequest request) throws TargetException {
        Target target = new Target();
        target.setSystemName(request.getSystemName())
                .setDescription(request.getDescription())
                .setOs(request.getOs())
                .setProjectId(request.getProjectId())
                .setCreatedAt(LocalDateTime.now());
        return target;
    }

    public Target updateTargetRequestToTarget(UpdateTargetRequest request, String targetId) throws TargetException {
        Target target = targetService.getById(targetId);
        if (request.getSystemName() != null) {
            target.setSystemName(request.getSystemName());
        }
        if (request.getDescription() != null) {
            target.setDescription(request.getDescription());
        }
        if (request.getOs() != null) {
            target.setOs(request.getOs());
        }
        if (request.getIpOrDomain() != null) {
            target.setIpOrDomain(request.getIpOrDomain());
        }
        if (request.getStatus() != null) {
            target.setStatus(request.getStatus());
        }
        if (request.getAssignedAgent() != null) {
            target.setAssignedAgent(request.getAssignedAgent());
        }
        target.setUpdatedAt(LocalDateTime.now());
        return target;
    }

    public TargetInfo targetToTargetInfo(Target target) {
        return modelMapper.map(target, TargetInfo.class);
    }

    public ResponseList<TargetInfo> targetsListToResponseList(TargetsList list) {
        List<TargetInfo> targetInfoList = targetListToTargetInfoList(list.getContent());
        PageImpl<TargetInfo> dtoPage = new PageImpl<>(targetInfoList, list.getPageable(),
                list.getTotalElements());
        return ListMapper.mapList(dtoPage);
    }

    private List<TargetInfo> targetListToTargetInfoList(List<Target> targets) {
        return targets
                .stream()
                .map(target -> modelMapper.map(target, TargetInfo.class))
                .collect(Collectors.toList());
    }
}