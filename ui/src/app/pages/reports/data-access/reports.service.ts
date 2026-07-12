import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  Report,
  ReportGenerateRequest,
  ReportHistoryResponse,
} from './reports.model';

@Injectable({
  providedIn: 'root',
})
export class ReportsService {
  private readonly apiUrl = `${environment.baseUrl}/api/reports`;

  constructor(private http: HttpClient) {}

  generate(request: ReportGenerateRequest): Observable<Report> {
    return this.http.post<Report>(this.apiUrl, request);
  }

  list(page = 0, size = 20): Observable<ReportHistoryResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ReportHistoryResponse>(this.apiUrl, { params });
  }

  get(id: string): Observable<Report> {
    return this.http.get<Report>(`${this.apiUrl}/${encodeURIComponent(id)}`);
  }
}
