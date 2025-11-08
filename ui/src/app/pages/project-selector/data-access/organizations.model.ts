export interface Organization {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  projectIds: string[];
  createdAt: Date;
  updatedAt?: Date;
}

export interface CreateOrganizationRequest {
  name: string;
  description?: string;
}

export interface UpdateOrganizationRequest {
  name: string;
  description?: string;
}

