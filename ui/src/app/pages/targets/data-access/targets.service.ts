import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from 'src/environments/environment';
import {
  CreateTargetRequest,
  UpdateTargetRequest,
  TargetInfo,
  TargetsList,
} from './targets.model';

const baseUrl = environment.baseUrl;

@Injectable()
export class TargetsService {
  constructor(private http: HttpClient) {}

  getTargets(page: number, size: number) {
    let params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<TargetsList>(`${baseUrl}/api/targets`, { params });
  }

  createTarget(targetRequest: CreateTargetRequest) {
    return this.http.post<TargetInfo>(`${baseUrl}/api/targets`, targetRequest);
  }

  deleteTarget(id: string) {
    return this.http.delete(`${baseUrl}/api/targets/${id}`);
  }

  getTargetById(targetId: string) {
    return this.http.get<TargetInfo>(`${baseUrl}/api/targets/${targetId}`);
  }

  updateById(request: UpdateTargetRequest, targetId: string) {
    return this.http.put<TargetInfo>(`${baseUrl}/api/targets/${targetId}`, request);
  }
}