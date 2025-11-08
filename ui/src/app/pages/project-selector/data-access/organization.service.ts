import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import {
  Organization,
  OrganizationInfo,
  CreateOrganizationRequest,
  UpdateOrganizationRequest
} from './organizations.model';

const baseUrl = environment.baseUrl;

@Injectable({
  providedIn: 'root',
})
export class OrganizationService {
  constructor(private http: HttpClient) {}

  getOrganizations(): Observable<OrganizationInfo[]> {
    return this.http.get<OrganizationInfo[]>(`${baseUrl}/api/organizations`);
  }

  getOrganizationById(id: string): Observable<OrganizationInfo> {
    return this.http.get<OrganizationInfo>(`${baseUrl}/api/organizations/${id}`);
  }

  createOrganization(request: CreateOrganizationRequest): Observable<OrganizationInfo> {
    return this.http.post<OrganizationInfo>(`${baseUrl}/api/organizations`, request);
  }

  updateOrganization(id: string, request: UpdateOrganizationRequest): Observable<OrganizationInfo> {
    return this.http.put<OrganizationInfo>(`${baseUrl}/api/organizations/${id}`, request);
  }

  deleteOrganization(id: string): Observable<void> {
    return this.http.delete<void>(`${baseUrl}/api/organizations/${id}`);
  }
}