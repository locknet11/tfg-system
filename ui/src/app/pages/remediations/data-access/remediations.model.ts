export type RemediationType =
  | 'SERVICE_UPDATE'
  | 'REBOOT_REQUIRED'
  | 'KERNEL_UPDATE'
  | 'UNKNOWN';

export type RemediationStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'SUCCESS'
  | 'FAILED'
  | 'PENDING_REBOOT'
  | 'SKIPPED';

export interface RemediationRecord {
  readonly id: string;
  readonly targetId: string;
  readonly targetIp: string;
  readonly agentId: string;
  readonly cveId: string;
  readonly serviceName: string;
  readonly serviceVersion: string;
  readonly remediationType: RemediationType;
  readonly status: RemediationStatus;
  readonly actionDescription: string | null;
  readonly preCheckLogs: string[];
  readonly executionLogs: string[];
  readonly postCheckLogs: string[];
  readonly errorMessage: string | null;
  readonly rollbackHint: string | null;
  readonly startedAt: string | null;
  readonly completedAt: string | null;
  readonly createdAt: string;
}

export interface RemediationListResponse {
  readonly content: RemediationRecord[];
  readonly totalElements: number;
}

export interface RecentActivity {
  readonly id: string;
  readonly cveId: string;
  readonly targetName: string | null;
  readonly status: string;
  readonly completedAt: string | null;
}

export interface RemediationStatistics {
  readonly totalCount: number;
  readonly byStatus: Record<string, number>;
  readonly meanTimeToRemediateSeconds: number;
  readonly recentActivity: RecentActivity[];
}
