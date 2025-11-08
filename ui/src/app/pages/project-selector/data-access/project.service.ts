import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import {
  Project,
  CreateProjectRequest,
  UpdateProjectRequest,
  UpdateProjectStatusRequest,
  AddMemberRequest
} from './projects.model';

const baseUrl = environment.baseUrl;

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  constructor(private http: HttpClient) {}

  getProjects(organizationId?: string): Observable<Project[]> {
    let params = new HttpParams();
    if (organizationId) {
      params = params.set('organizationId', organizationId);
    }
    return this.http.get<Project[]>(`${baseUrl}/api/projects`, { params });
  }

  getProjectById(id: string): Observable<Project> {
    return this.http.get<Project>(`${baseUrl}/api/projects/${id}`);
  }

  createProject(request: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(`${baseUrl}/api/projects`, request);
  }

  updateProject(id: string, request: UpdateProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${baseUrl}/api/projects/${id}`, request);
  }

  deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`${baseUrl}/api/projects/${id}`);
  }

  updateProjectStatus(id: string, request: UpdateProjectStatusRequest): Observable<void> {
    return this.http.put<void>(`${baseUrl}/api/projects/${id}/status`, request);
  }

  addMember(projectId: string, request: AddMemberRequest): Observable<void> {
    return this.http.post<void>(`${baseUrl}/api/projects/${projectId}/members`, request);
  }

  removeMember(projectId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${baseUrl}/api/projects/${projectId}/members/${userId}`);
  }
}