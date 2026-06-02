export interface Project {
  id: string;
  name: string;
  projectIdentifier: string;
  description?: string;
  organizationId: string;
  status: ProjectStatus;
  replicationPolicy?: ReplicationPolicy;
  createdAt: string;
  updatedAt?: string;
}

export interface ProjectInfo {
  id: string;
  name: string;
  projectIdentifier: string;
  description?: string;
  organizationId: string;
  status: ProjectStatus;
  replicationPolicy?: ReplicationPolicy;
  createdAt: string;
  updatedAt?: string;
}

export interface ReplicationPolicy {
  mode: string;
  minSeverity?: string | null;
  notifyAdmin: boolean;
}

export interface UpdateReplicationPolicyRequest {
  mode: string;
  minSeverity?: string | null;
  notifyAdmin: boolean;
}

export type ProjectStatus = 'ACTIVE' | 'INACTIVE' | 'COMPLETED' | 'ARCHIVED';

export interface CreateProjectRequest {
  name: string;
  description?: string;
  organizationId: string;
}

export interface UpdateProjectRequest {
  name: string;
  description?: string;
}

export interface UpdateProjectStatusRequest {
  status: ProjectStatus;
}
