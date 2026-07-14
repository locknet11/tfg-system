package com.spulido.tfg.domain.report.services.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.common.exception.ErrorCode;
import com.spulido.tfg.domain.remediation.db.RemediationRecordRepository;
import com.spulido.tfg.domain.remediation.model.RemediationRecord;
import com.spulido.tfg.domain.remediation.model.RemediationStatus;
import com.spulido.tfg.domain.remediation.util.RemediationMetrics;
import com.spulido.tfg.domain.report.db.ReportRepository;
import com.spulido.tfg.domain.report.exception.ReportException;
import com.spulido.tfg.domain.report.model.GenerationType;
import com.spulido.tfg.domain.report.model.Report;
import com.spulido.tfg.domain.report.model.ReportFilters;
import com.spulido.tfg.domain.report.model.ReportItem;
import com.spulido.tfg.domain.report.model.ReportSummary;
import com.spulido.tfg.domain.report.model.dto.ReportGenerateRequest;
import com.spulido.tfg.domain.report.model.dto.ReportInfo;
import com.spulido.tfg.domain.report.services.ReportService;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.vulnerability.db.ServiceVulnerabilityRepository;
import com.spulido.tfg.domain.vulnerability.model.CveEntry;
import com.spulido.tfg.domain.vulnerability.model.ServiceVulnerabilityRecord;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private static final String UNKNOWN_SEVERITY = "UNKNOWN";
    private static final List<String> SEVERITY_ORDER = List.of("CRITICAL", "HIGH", "MEDIUM", "LOW", UNKNOWN_SEVERITY);
    private static final int TARGET_PAGE_SIZE = 1000;

    private final ReportRepository reportRepository;
    private final RemediationRecordRepository remediationRepository;
    private final ServiceVulnerabilityRepository vulnerabilityRepository;
    private final TargetRepository targetRepository;

    public ReportServiceImpl(ReportRepository reportRepository,
            RemediationRecordRepository remediationRepository,
            ServiceVulnerabilityRepository vulnerabilityRepository,
            TargetRepository targetRepository) {
        this.reportRepository = Objects.requireNonNull(reportRepository, "reportRepository must not be null");
        this.remediationRepository = Objects.requireNonNull(remediationRepository,
                "remediationRepository must not be null");
        this.vulnerabilityRepository = Objects.requireNonNull(vulnerabilityRepository,
                "vulnerabilityRepository must not be null");
        this.targetRepository = Objects.requireNonNull(targetRepository, "targetRepository must not be null");
    }

    @Override
    public Report generate(ReportGenerateRequest request, GenerationType generationType) {
        String orgId = ProjectContext.getOrganizationId();
        String projectId = ProjectContext.getProjectId();
        if (isBlank(orgId) || isBlank(projectId)) {
            throw new ReportException("No organization/project selected",
                    ErrorCode.REPORT_NO_PROJECT_CONTEXT);
        }

        ReportGenerateRequest filters = request != null ? request : ReportGenerateRequest.builder().build();

        TargetIndex targetIndex = buildTargetIndex();
        List<RemediationRecord> records = loadRecords(orgId, projectId, filters);
        records = applyInMemoryFilters(records, filters, targetIndex);

        Map<String, CveEntry> cveIndex = buildCveIndex(records);

        List<ReportItem> items = new ArrayList<>();
        List<RemediationRecord> kept = new ArrayList<>();
        List<String> severityFilter = normalizeSeverities(filters.getSeverities());
        for (RemediationRecord record : records) {
            ReportItem item = toItem(record, cveIndex, targetIndex);
            if (!severityFilter.isEmpty() && !severityFilter.contains(item.getSeverity())) {
                continue;
            }
            items.add(item);
            kept.add(record);
        }

        if (items.isEmpty()) {
            throw new ReportException("No data matches the selected filters",
                    ErrorCode.REPORT_EMPTY_RESULT);
        }

        ReportSummary summary = buildSummary(kept, items);
        LocalDateTime now = LocalDateTime.now();
        Instant generatedAt = Instant.now();

        Report report = Report.builder()
                .organizationId(orgId)
                .projectId(projectId)
                .title(buildTitle(projectId, now))
                .generationType(generationType)
                .generatedAt(generatedAt)
                .generatedBy(resolveGeneratedBy(generationType))
                .filters(toFilters(filters))
                .summary(summary)
                .items(items)
                .build();
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        Report saved = reportRepository.save(report);
        log.info("Generated {} report {} for org={} project={} with {} items",
                generationType, saved.getId(), orgId, projectId, items.size());
        return saved;
    }

    @Override
    public Page<ReportInfo> findHistory(Pageable pageable) {
        String orgId = ProjectContext.getOrganizationId();
        String projectId = ProjectContext.getProjectId();
        if (isBlank(orgId) || isBlank(projectId)) {
            throw new ReportException("No organization/project selected",
                    ErrorCode.REPORT_NO_PROJECT_CONTEXT);
        }
        return reportRepository.findByOrganizationIdAndProjectId(orgId, projectId, pageable)
                .map(this::toInfo);
    }

    @Override
    public Report findById(String id) {
        String orgId = ProjectContext.getOrganizationId();
        String projectId = ProjectContext.getProjectId();
        if (isBlank(orgId) || isBlank(projectId)) {
            throw new ReportException("No organization/project selected",
                    ErrorCode.REPORT_NO_PROJECT_CONTEXT);
        }
        return reportRepository.findByIdAndOrganizationIdAndProjectId(id, orgId, projectId)
                .orElseThrow(() -> new ReportException("Report not found: " + id,
                        ErrorCode.REPORT_NOT_FOUND));
    }

    private List<RemediationRecord> loadRecords(String orgId, String projectId, ReportGenerateRequest filters) {
        if (filters.getFrom() != null && filters.getTo() != null) {
            return remediationRepository.findByOrganizationIdAndProjectIdAndCompletedAtBetween(
                    orgId, projectId, filters.getFrom(), filters.getTo());
        }
        return remediationRepository.findByOrganizationIdAndProjectId(orgId, projectId);
    }

    private List<RemediationRecord> applyInMemoryFilters(List<RemediationRecord> records,
            ReportGenerateRequest filters, TargetIndex targetIndex) {
        String targetId = filters.getTargetId();
        Target selectedTarget = targetIndex.selected(targetId);
        String selectedHost = selectedTarget != null ? canonicalHost(selectedTarget.getIpOrDomain()) : null;
        List<RemediationStatus> statuses = filters.getStatuses();
        Instant from = filters.getFrom();
        Instant to = filters.getTo();
        boolean bothBounds = from != null && to != null;

        return records.stream()
                // A stored targetId is a host/IP for agent-reported records but a Target id for others, so a
                // target-scoped report matches either the Target's id or its host/IP (loopback forms such as
                // 127.0.0.1 and ::1 are treated as equivalent).
                .filter(r -> targetId == null || targetId.isBlank()
                        || targetId.equals(r.getTargetId())
                        || (selectedHost != null && selectedHost.equals(canonicalHost(r.getTargetId()))))
                .filter(r -> statuses == null || statuses.isEmpty() || statuses.contains(r.getStatus()))
                // Single-sided date bounds (both-bounds case already handled by the query).
                .filter(r -> bothBounds || from == null
                        || (r.getCompletedAt() != null && !r.getCompletedAt().isBefore(from)))
                .filter(r -> bothBounds || to == null
                        || (r.getCompletedAt() != null && !r.getCompletedAt().isAfter(to)))
                .collect(Collectors.toList());
    }

    private Map<String, CveEntry> buildCveIndex(List<RemediationRecord> records) {
        boolean hasCve = records.stream().anyMatch(r -> r.getCveId() != null && !r.getCveId().isBlank());
        if (!hasCve) {
            return Map.of();
        }
        Map<String, CveEntry> index = new HashMap<>();
        for (ServiceVulnerabilityRecord svr : vulnerabilityRepository.findAll()) {
            if (svr.getCves() == null) {
                continue;
            }
            for (CveEntry cve : svr.getCves()) {
                if (cve != null && cve.getCveId() != null) {
                    index.putIfAbsent(cve.getCveId(), cve);
                }
            }
        }
        return index;
    }

    private ReportItem toItem(RemediationRecord record, Map<String, CveEntry> cveIndex,
            TargetIndex targetIndex) {
        CveEntry cve = record.getCveId() != null ? cveIndex.get(record.getCveId()) : null;
        String severity = cve != null ? normalizeSeverity(cve.getSeverity()) : UNKNOWN_SEVERITY;
        Double cvss = cve != null ? cve.getCvssScore() : null;

        return ReportItem.builder()
                .cveId(record.getCveId())
                .severity(severity)
                .cvssScore(cvss)
                .targetId(record.getTargetId())
                .targetName(targetIndex.resolveName(record.getTargetId()))
                .remediationStatus(record.getStatus() != null ? record.getStatus().name() : null)
                .startedAt(record.getStartedAt())
                .completedAt(record.getCompletedAt())
                .build();
    }

    private TargetIndex buildTargetIndex() {
        Map<String, Target> byId = new HashMap<>();
        Map<String, Target> byHost = new HashMap<>();
        for (Target target : targetRepository.findAllScoped(Pageable.ofSize(TARGET_PAGE_SIZE)).getContent()) {
            if (target.getId() != null) {
                byId.putIfAbsent(target.getId(), target);
            }
            String host = canonicalHost(target.getIpOrDomain());
            if (host != null) {
                byHost.putIfAbsent(host, target);
            }
        }
        return new TargetIndex(byId, byHost);
    }

    /**
     * Normalizes a host/IP so different spellings of the same address join together, notably the loopback
     * forms (127.0.0.0/8, ::1, its expanded 0:0:0:0:0:0:0:1, and localhost) which agents and target
     * registration may record differently. Returns {@code null} for blank input.
     */
    private static String canonicalHost(String value) {
        if (value == null) {
            return null;
        }
        String host = value.trim().toLowerCase();
        if (host.isEmpty()) {
            return null;
        }
        if (host.equals("localhost") || host.startsWith("127.")
                || host.equals("::1") || host.equals("0:0:0:0:0:0:0:1")) {
            return "127.0.0.1";
        }
        return host;
    }

    /**
     * In-memory view of the active project's targets, indexed by id and by canonical host/IP so a report can
     * join remediation records (which store a host/IP) to targets (referenced by id in the UI) with id taking
     * precedence over host/IP.
     */
    private static final class TargetIndex {
        private final Map<String, Target> byId;
        private final Map<String, Target> byHost;

        private TargetIndex(Map<String, Target> byId, Map<String, Target> byHost) {
            this.byId = byId;
            this.byHost = byHost;
        }

        private Target selected(String targetId) {
            return targetId != null && !targetId.isBlank() ? byId.get(targetId) : null;
        }

        private String resolveName(String targetId) {
            if (targetId == null || targetId.isBlank()) {
                return null;
            }
            Target target = byId.get(targetId);
            if (target == null) {
                target = byHost.get(canonicalHost(targetId));
            }
            return target != null ? target.getSystemName() : null;
        }
    }

    private ReportSummary buildSummary(List<RemediationRecord> records, List<ReportItem> items) {
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (String severity : SEVERITY_ORDER) {
            bySeverity.put(severity, 0L);
        }
        for (ReportItem item : items) {
            bySeverity.merge(item.getSeverity(), 1L, Long::sum);
        }

        Map<String, Long> byStatus = records.stream()
                .filter(r -> r.getStatus() != null)
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));

        long targetsCovered = records.stream()
                .map(RemediationRecord::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long distinctCves = records.stream()
                .map(RemediationRecord::getCveId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return ReportSummary.builder()
                .vulnerabilitiesBySeverity(bySeverity)
                .remediationsByStatus(byStatus)
                .meanTimeToRemediateSeconds(RemediationMetrics.meanTimeToRemediateSeconds(records))
                .targetsCovered((int) targetsCovered)
                .totalVulnerabilities(distinctCves)
                .totalRemediations(records.size())
                .build();
    }

    private ReportInfo toInfo(Report report) {
        ReportSummary summary = report.getSummary();
        return ReportInfo.builder()
                .id(report.getId())
                .title(report.getTitle())
                .generationType(report.getGenerationType())
                .generatedAt(report.getGeneratedAt())
                .generatedBy(report.getGeneratedBy())
                .totalVulnerabilities(summary != null ? summary.getTotalVulnerabilities() : 0L)
                .totalRemediations(summary != null ? summary.getTotalRemediations() : 0L)
                .targetsCovered(summary != null ? summary.getTargetsCovered() : 0)
                .build();
    }

    private ReportFilters toFilters(ReportGenerateRequest request) {
        return ReportFilters.builder()
                .targetId(request.getTargetId())
                .from(request.getFrom())
                .to(request.getTo())
                .severities(normalizeSeverities(request.getSeverities()))
                .statuses(request.getStatuses() != null ? request.getStatuses() : List.of())
                .build();
    }

    private List<String> normalizeSeverities(List<String> severities) {
        if (severities == null || severities.isEmpty()) {
            return List.of();
        }
        return severities.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeSeverity)
                .collect(Collectors.toList());
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return UNKNOWN_SEVERITY;
        }
        String upper = severity.trim().toUpperCase();
        return SEVERITY_ORDER.contains(upper) ? upper : UNKNOWN_SEVERITY;
    }

    private String buildTitle(String projectId, LocalDateTime when) {
        return "Security report — " + projectId + " — " + when.toLocalDate();
    }

    private String resolveGeneratedBy(GenerationType generationType) {
        if (generationType == GenerationType.SCHEDULED) {
            return "system";
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return "unknown";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
