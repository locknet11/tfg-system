import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ReplicationRequestsList } from './replication-requests.model';

@Injectable({ providedIn: 'root' })
export class ReplicationRequestsService {
  private readonly baseUrl = `${environment.baseUrl}/api/replication-requests`;

  constructor(private readonly http: HttpClient) {}

  list(
    page: number,
    size: number,
    status?: string,
    severity?: string
  ): Observable<ReplicationRequestsList> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (severity) params = params.set('severity', severity);
    return this.http.get<ReplicationRequestsList>(this.baseUrl, { params });
  }

  approve(id: string): Observable<unknown> {
    return this.http.put(`${this.baseUrl}/${id}/approve`, {});
  }

  deny(id: string): Observable<unknown> {
    return this.http.put(`${this.baseUrl}/${id}/deny`, {});
  }
}
