import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map } from 'rxjs';
import { environment } from 'src/environments/environment';
import {
  AgentsList,
  AgentPlanInfo,
  AssignPlanRequest,
  Agent,
  AgentPlatformInfo,
  AgentMetrics,
} from './agents.model';

const baseUrl = environment.baseUrl;

@Injectable({ providedIn: 'root' })
export class AgentsService {
  constructor(private http: HttpClient) {}

  list(query: string, page: number, size: number) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query) {
      params = params.set('query', query);
    }
    return this.http.get<AgentsList>(`${baseUrl}/api/agent`, { params });
  }

  delete(id: string) {
    return this.http.delete(`${baseUrl}/api/agent/${id}`);
  }

  assignPlan(agentId: string, request: AssignPlanRequest) {
    return this.http.put<Agent>(
      `${baseUrl}/api/agent/${agentId}/plan`,
      request
    );
  }

  getPlan(agentId: string) {
    return this.http
      .get<AgentPlanInfo | null>(`${baseUrl}/api/agent/${agentId}/plan`)
      .pipe(map(plan => plan ?? null));
  }

  getDownloadPlatforms() {
    return this.http.get<AgentPlatformInfo[]>(
      `${baseUrl}/api/agent/download/platforms`
    );
  }

  downloadAgent(platform: string): void {
    window.open(`${baseUrl}/api/agent/download/${platform}`, '_blank');
  }

  getMetrics() {
    return this.http.get<AgentMetrics>(`${baseUrl}/api/agent/metrics`);
  }
}
