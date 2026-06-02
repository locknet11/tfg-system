export interface ReplicationRequest {
  id: string;
  parentAgentId: string;
  parentAgentName: string;
  targetIp: string;
  targetPort: number;
  exploitId: string;
  cveId: string;
  serviceName: string;
  serviceVersion: string;
  severity: string;
  status: ReplicationRequestStatus;
  policy: string;
  approvedBy: string | null;
  createdAt: string;
  expiresAt: string | null;
  resolvedAt: string | null;
}

export enum ReplicationRequestStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  DENIED = 'DENIED',
  EXPIRED = 'EXPIRED',
}

export interface ReplicationRequestsList {
  content: ReplicationRequest[];
  totalPages: number;
  totalElements: number;
  page: number;
  size: number;
}

export interface ReplicationStatusResponse {
  status: string;
  replicationToken?: string;
  downloadUrl?: string;
  preauthCode?: string;
  centralUrl?: string;
}
