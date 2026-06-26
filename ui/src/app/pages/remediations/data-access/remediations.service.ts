import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  RemediationListResponse,
  RemediationRecord,
  RemediationStatistics,
  RemediationStatus,
} from './remediations.model';

@Injectable({
  providedIn: 'root',
})
export class RemediationsService {
  private readonly apiUrl = `${environment.baseUrl}/api/remediations`;

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
}
