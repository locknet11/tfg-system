import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from 'src/environments/environment';
import { AgentsList } from './agents.model';

const baseUrl = environment.baseUrl;

@Injectable()
export class AgentsService {
  constructor(private http: HttpClient) {}

  list(page: number, size: number) {
    let params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<AgentsList>(`${baseUrl}/api/agent`, { params });
  }

  delete(id: string) {
    return this.http.delete(`${baseUrl}/api/agent/${id}`);
  }
}
