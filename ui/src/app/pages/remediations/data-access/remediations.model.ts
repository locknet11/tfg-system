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

export interface RemediationStatistics {
  readonly totalRemediations: number;
  readonly successCount: number;
  readonly failedCount: number;
  readonly pendingCount: number;
  readonly skippedCount: number;
  readonly pendingRebootCount: number;
}
