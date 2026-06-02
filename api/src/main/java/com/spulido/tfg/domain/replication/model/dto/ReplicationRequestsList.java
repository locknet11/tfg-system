package com.spulido.tfg.domain.replication.model.dto;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.replication.model.ReplicationRequest;

import java.util.List;

public class ReplicationRequestsList extends PageImpl<ReplicationRequest> {

    public ReplicationRequestsList(List<ReplicationRequest> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }
}
