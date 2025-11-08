export interface Project {
  id: string;
  name: string;
  description?: string;
  organizationId: string;
  status: ProjectStatus;
  createdAt: string;
  updatedAt?: string;
}

export interface ProjectInfo {
  id: string;
  name: string;
  description?: string;
  organizationId: string;
  status: ProjectStatus;
  createdAt: string;
  updatedAt?: string;
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

