import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CriticalVulnerability,
  ChartData,
  DashboardKPIs,
  VulnerabilityTrendPoint,
} from './dashboard.model';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly apiUrl = `${environment.baseUrl}/api/dashboard`;

  constructor(private http: HttpClient) {}

  getKPIs(): Observable<DashboardKPIs> {
    return this.http.get<DashboardKPIs>(`${this.apiUrl}/kpis`);
  }

  getCriticalVulnerabilities(): Observable<CriticalVulnerability[]> {
    return this.http.get<CriticalVulnerability[]>(
      `${this.apiUrl}/critical-vulnerabilities`
    );
  }

  getVulnerabilityTrend(months = 6): Observable<VulnerabilityTrendPoint[]> {
    const params = new HttpParams().set('months', months.toString());
    return this.http.get<VulnerabilityTrendPoint[]>(
      `${this.apiUrl}/vulnerability-trend`,
      { params }
    );
  }

  getVulnerabilitiesChartData(months = 6): Observable<ChartData> {
    return this.getVulnerabilityTrend(months).pipe(
      map((trend: VulnerabilityTrendPoint[]) => {
        const labels = trend.map((t) => t.period);
        const data = trend.map((t) => t.count);
        return {
          labels,
          datasets: [
            {
              label: 'Vulnerabilities Detected',
              data,
              backgroundColor: [
                'rgba(255, 99, 132, 0.2)',
                'rgba(54, 162, 235, 0.2)',
                'rgba(75, 192, 192, 0.2)',
                'rgba(255, 206, 86, 0.2)',
                'rgba(153, 102, 255, 0.2)',
                'rgba(255, 159, 64, 0.2)',
              ],
              borderColor: 'rgba(255, 99, 132, 1)',
              fill: false,
            },
          ],
        };
      })
    );
  }
}
