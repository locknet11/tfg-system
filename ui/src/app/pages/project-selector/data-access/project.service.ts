import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import {
  Project,
  ProjectInfo,
  CreateProjectRequest,
  UpdateProjectRequest,
  UpdateProjectStatusRequest
} from './projects.model';

const baseUrl = environment.baseUrl;

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  constructor(private http: HttpClient) {}

  getProjects(organizationId?: string): Observable<ProjectInfo[]> {
    let params = new HttpParams();
    if (organizationId) {
      params = params.set('organizationId', organizationId);
    }
    return this.http.get<ProjectInfo[]>(`${baseUrl}/api/projects`, { params });
  }

  getProjectById(id: string): Observable<ProjectInfo> {
    return this.http.get<ProjectInfo>(`${baseUrl}/api/projects/${id}`);
  }

  createProject(request: CreateProjectRequest): Observable<ProjectInfo> {
    return this.http.post<ProjectInfo>(`${baseUrl}/api/projects`, request);
  }

  updateProject(id: string, request: UpdateProjectRequest): Observable<ProjectInfo> {
    return this.http.put<ProjectInfo>(`${baseUrl}/api/projects/${id}`, request);
  }

  deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`${baseUrl}/api/projects/${id}`);
  }

  updateProjectStatus(id: string, request: UpdateProjectStatusRequest): Observable<void> {
    return this.http.put<void>(`${baseUrl}/api/projects/${id}/status`, request);
  }
}