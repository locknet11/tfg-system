export interface Target {
  id: string;
  systemName: string;
  description?: string;
  os: OperatingSystem;
  uniqueId: string;
  projectId: string;
  ipOrDomain?: string;
  status: TargetStatus;
  assignedAgent?: string;
}

export enum TargetStatus {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE',
  IN_REVIEW = 'IN_REVIEW',
}

export enum OperatingSystem {
  LINUX = 'LINUX'
}

export interface TargetsList {
  content: TargetInfo[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

export interface TargetInfo {
  id: string;
  systemName: string;
  description?: string;
  os: OperatingSystem;
  uniqueId: string;
  projectId: string;
  ipOrDomain?: string;
  status: TargetStatus;
  assignedAgent?: string;
}

export interface CreateTargetRequest {
  systemName: string;
  description?: string;
  os: OperatingSystem;
  projectId: string;
}

export interface UpdateTargetRequest {
  systemName?: string;
  description?: string;
  os?: OperatingSystem;
  ipOrDomain?: string;
  status?: TargetStatus;
  assignedAgent?: string;
}
