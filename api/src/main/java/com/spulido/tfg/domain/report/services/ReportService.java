package com.spulido.tfg.domain.report.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.report.model.GenerationType;
import com.spulido.tfg.domain.report.model.Report;
import com.spulido.tfg.domain.report.model.dto.ReportGenerateRequest;
import com.spulido.tfg.domain.report.model.dto.ReportInfo;

/**
 * Service for generating and retrieving immutable report snapshots, scoped to
 * the active organization/project from {@link com.spulido.tfg.common.context.ProjectContext}.
 */
public interface ReportService {

    /**
     * Generate and persist an immutable report snapshot for the active
     * organization/project, applying the given filters.
     *
     * @param request        optional filters
     * @param generationType how the report is being produced
     * @return the persisted report
     * @throws com.spulido.tfg.domain.report.exception.ReportException with
     *         {@code REPORT_NO_PROJECT_CONTEXT} if no project is selected, or
     *         {@code REPORT_EMPTY_RESULT} if no data matches (nothing is persisted)
     */
    Report generate(ReportGenerateRequest request, GenerationType generationType);

    /**
     * Paged history for the active organization/project, newest first.
     */
    Page<ReportInfo> findHistory(Pageable pageable);

    /**
     * Full stored snapshot by id, tenant-scoped to the active organization/project.
     *
     * @throws com.spulido.tfg.domain.report.exception.ReportException with
     *         {@code REPORT_NOT_FOUND} when absent in the caller's scope
     */
    Report findById(String id);
}
