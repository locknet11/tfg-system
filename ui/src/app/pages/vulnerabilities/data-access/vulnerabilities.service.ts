import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  VulnerabilityListResponse,
  VulnerabilityLookupResponse,
} from './vulnerabilities.model';

@Injectable({
  providedIn: 'root',
})
export class VulnerabilitiesService {
  private readonly apiUrl = `${environment.baseUrl}/api/vulnerabilities`;

  constructor(private http: HttpClient) {}

  list(
    query = '',
    page = 0,
    size = 10,
    severity?: string
  ): Observable<VulnerabilityListResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (query) {
      params = params.set('query', query);
    }
    if (severity) {
      params = params.set('severity', severity);
    }

    return this.http.get<VulnerabilityListResponse>(this.apiUrl, { params });
  }

  get(serviceKey: string): Observable<VulnerabilityLookupResponse> {
    return this.http.get<VulnerabilityLookupResponse>(
      `${this.apiUrl}/${encodeURIComponent(serviceKey)}`
    );
  }

  refresh(serviceKey: string): Observable<VulnerabilityLookupResponse> {
    return this.http.post<VulnerabilityLookupResponse>(
      `${this.apiUrl}/${encodeURIComponent(serviceKey)}/refresh`,
      {}
    );
  }
}
