import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PlanTemplate } from './templates.model';

@Injectable({
  providedIn: 'root',
})
export class TemplatesService {
  private readonly apiUrl = `${environment.baseUrl}/api/templates`;

  constructor(private http: HttpClient) {}

  list(
    query = '',
    page = 0,
    size = 10
  ): Observable<{ content: PlanTemplate[]; totalElements: number }> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<{ content: PlanTemplate[]; totalElements: number }>(
      this.apiUrl,
      { params }
    );
  }

  get(id: string): Observable<PlanTemplate> {
    return this.http.get<PlanTemplate>(`${this.apiUrl}/${id}`);
  }

  create(template: Partial<PlanTemplate>): Observable<PlanTemplate> {
    return this.http.post<PlanTemplate>(this.apiUrl, template);
  }

  update(
    id: string,
    template: Partial<PlanTemplate>
  ): Observable<PlanTemplate> {
    return this.http.put<PlanTemplate>(`${this.apiUrl}/${id}`, template);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
