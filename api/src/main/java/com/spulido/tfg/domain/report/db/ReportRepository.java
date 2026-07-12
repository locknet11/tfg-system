package com.spulido.tfg.domain.report.db;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.spulido.tfg.domain.report.model.Report;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {

    Page<Report> findByOrganizationIdAndProjectId(String organizationId, String projectId, Pageable pageable);

    Optional<Report> findByIdAndOrganizationIdAndProjectId(String id, String organizationId, String projectId);
}
