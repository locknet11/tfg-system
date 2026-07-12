import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  RemediationListResponse,
  RemediationRecord,
  RemediationStatistics,
  RemediationStatus,
  RemediationStrategy,
  StrategyListResponse,
} from './remediations.model';

@Injectable({
  providedIn: 'root',
})
export class RemediationsService {
  private readonly apiUrl = `${environment.baseUrl}/api/remediations`;
  private readonly strategiesUrl = `${environment.baseUrl}/api/remediation-strategies`;

  constructor(private http: HttpClient) {}

  list(
    page = 0,
    size = 10,
    status?: RemediationStatus,
    targetId?: string
  ): Observable<RemediationListResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }
    if (targetId) {
      params = params.set('targetId', targetId);
    }

    return this.http.get<RemediationListResponse>(this.apiUrl, { params });
  }

  get(id: string): Observable<RemediationRecord> {
    return this.http.get<RemediationRecord>(
      `${this.apiUrl}/${encodeURIComponent(id)}`
    );
  }

  getStatistics(): Observable<RemediationStatistics> {
    return this.http.get<RemediationStatistics>(`${this.apiUrl}/statistics`);
  }

  listStrategies(
    page = 0,
    size = 20,
    cveId?: string,
    operatingSystem?: string,
    packageName?: string,
    remediationType?: string,
    action?: string
  ): Observable<StrategyListResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (cveId) {
      params = params.set('cveId', cveId);
    }
    if (operatingSystem) {
      params = params.set('operatingSystem', operatingSystem);
    }
    if (packageName) {
      params = params.set('packageName', packageName);
    }
    if (remediationType) {
      params = params.set('remediationType', remediationType);
    }
    if (action) {
      params = params.set('action', action);
    }

    return this.http.get<StrategyListResponse>(this.strategiesUrl, { params });
  }

  getStrategy(id: string): Observable<RemediationStrategy> {
    return this.http.get<RemediationStrategy>(
      `${this.strategiesUrl}/${encodeURIComponent(id)}`
    );
  }
}
