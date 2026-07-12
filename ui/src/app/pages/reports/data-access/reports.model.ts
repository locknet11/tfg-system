import { RemediationStatus } from '../../remediations/data-access/remediations.model';

export type ReportSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN';

export type GenerationType = 'ON_DEMAND' | 'SCHEDULED';

export interface ReportFilters {
  readonly targetId: string | null;
  readonly from: string | null;
  readonly to: string | null;
  readonly severities: ReportSeverity[];
  readonly statuses: RemediationStatus[];
}

export interface ReportSummary {
  readonly vulnerabilitiesBySeverity: Readonly<Record<string, number>>;
  readonly remediationsByStatus: Readonly<Record<string, number>>;
  readonly meanTimeToRemediateSeconds: number;
  readonly targetsCovered: number;
  readonly totalVulnerabilities: number;
  readonly totalRemediations: number;
}

export interface ReportItem {
  readonly cveId: string;
  readonly severity: ReportSeverity | null;
  readonly cvssScore: number | null;
  readonly targetId: string;
  readonly targetName: string | null;
  readonly remediationStatus: RemediationStatus;
  readonly startedAt: string | null;
  readonly completedAt: string | null;
}

export interface Report {
  readonly id: string;
  readonly organizationId: string;
  readonly projectId: string;
  readonly title: string;
  readonly generationType: GenerationType;
  readonly generatedAt: string;
  readonly generatedBy: string;
  readonly createdAt: string;
  readonly filters: ReportFilters;
  readonly summary: ReportSummary;
  readonly items: ReportItem[];
}

export interface ReportInfo {
  readonly id: string;
  readonly title: string;
  readonly generationType: GenerationType;
  readonly generatedAt: string;
  readonly generatedBy: string;
  readonly totalVulnerabilities: number;
  readonly totalRemediations: number;
  readonly targetsCovered: number;
}

export interface ReportHistoryResponse {
  readonly content: ReportInfo[];
  readonly totalElements: number;
}

export interface ReportGenerateRequest {
  readonly targetId?: string | null;
  readonly from?: string | null;
  readonly to?: string | null;
  readonly severities?: ReportSeverity[];
  readonly statuses?: RemediationStatus[];
}

/** Application error code returned when a generation matched no data. */
export const REPORT_EMPTY_RESULT = 'REPORT_EMPTY_RESULT';
