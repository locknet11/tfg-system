import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import {
  Organization,
  CreateOrganizationRequest,
  UpdateOrganizationRequest,
  AddMemberRequest
} from './organizations.model';

const baseUrl = environment.baseUrl;

@Injectable({
  providedIn: 'root',
})
export class OrganizationService {
  constructor(private http: HttpClient) {}

  getOrganizations(): Observable<Organization[]> {
    return this.http.get<Organization[]>(`${baseUrl}/api/organizations`);
  }

  getOrganizationById(id: string): Observable<Organization> {
    return this.http.get<Organization>(`${baseUrl}/api/organizations/${id}`);
  }

  createOrganization(request: CreateOrganizationRequest): Observable<Organization> {
    return this.http.post<Organization>(`${baseUrl}/api/organizations`, request);
  }

  updateOrganization(id: string, request: UpdateOrganizationRequest): Observable<Organization> {
    return this.http.put<Organization>(`${baseUrl}/api/organizations/${id}`, request);
  }

  deleteOrganization(id: string): Observable<void> {
    return this.http.delete<void>(`${baseUrl}/api/organizations/${id}`);
  }

  addMember(organizationId: string, request: AddMemberRequest): Observable<void> {
    return this.http.post<void>(`${baseUrl}/api/organizations/${organizationId}/members`, request);
  }

  removeMember(organizationId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${baseUrl}/api/organizations/${organizationId}/members/${userId}`);
  }
}