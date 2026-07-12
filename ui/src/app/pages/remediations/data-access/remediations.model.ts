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

export type RemediationAction =
  | 'APT_UPGRADE'
  | 'APT_INSTALL'
  | 'CONFIG_UPDATE'
  | 'SYSTEMCTL_RESTART'
  | 'MANUAL';

export interface RemediationStrategy {
  readonly id: string;
  readonly cveId: string;
  readonly operatingSystem: string;
  readonly packageName: string;
  readonly remediationType: RemediationType;
  readonly action: RemediationAction;
  readonly targetVersion: string;
  readonly preCheckCommands: readonly string[];
  readonly fixCommands: readonly string[];
  readonly postCheckCommands: readonly string[];
  readonly serviceName: string | null;
  readonly requiresReboot: boolean;
  readonly notes: string;
}

export interface StrategyListResponse {
  readonly content: readonly RemediationStrategy[];
  readonly totalElements: number;
}

export interface RemediationStatistics {
  readonly totalCount: number;
  readonly byStatus: Record<string, number>;
  readonly meanTimeToRemediateSeconds: number;
  readonly recentActivity: RecentActivity[];
}
