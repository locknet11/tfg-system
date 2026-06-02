package com.spulido.tfg.domain.replication.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplicationRequestResponse {

    private String id;

    private String status;

    private String replicationToken;

    private String downloadUrl;

    private String preauthCode;

    private String centralUrl;
}
