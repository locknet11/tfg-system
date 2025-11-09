export interface Organization {
  id: string;
  name: string;
  organizationIdentifier: string;
  description?: string;
  ownerId: string;
  projectIds: string[];
  createdAt: string;
  updatedAt?: string;
}

export interface OrganizationInfo {
  id: string;
  name: string;
  organizationIdentifier: string;
  description?: string;
  ownerId: string;
  projectIds: string[];
  createdAt: string;
  updatedAt?: string;
}

export interface CreateOrganizationRequest {
  name: string;
  description?: string;
}

export interface UpdateOrganizationRequest {
  name: string;
  description?: string;
}

