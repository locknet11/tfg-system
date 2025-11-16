import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { AlertConfiguration } from './alerts.model';

@Injectable({
  providedIn: 'root',
})
export class AlertsService {
  private readonly apiUrl = `${environment.baseUrl}/api/alerts`;

  constructor(private http: HttpClient) {}

  list(
    page = 0,
    size = 10
  ): Observable<{ content: AlertConfiguration[]; totalElements: number }> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<{
      content: AlertConfiguration[];
      totalElements: number;
    }>(this.apiUrl, { params });
  }

  get(id: string): Observable<AlertConfiguration> {
    return this.http.get<AlertConfiguration>(`${this.apiUrl}/${id}`);
  }

  create(alert: Partial<AlertConfiguration>): Observable<AlertConfiguration> {
    return this.http.post<AlertConfiguration>(this.apiUrl, alert);
  }

  update(
    id: string,
    alert: Partial<AlertConfiguration>
  ): Observable<AlertConfiguration> {
    return this.http.put<AlertConfiguration>(`${this.apiUrl}/${id}`, alert);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
