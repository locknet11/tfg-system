import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { DashboardKPIs, CriticalVulnerability, ChartData } from './dashboard.model';

@Injectable()
export class DashboardService {
  getKPIs(): Observable<DashboardKPIs> {
    // Moquear todos los KPIs
    const mockKPIs: DashboardKPIs = {
      targetsCount: 0, // Se obtendrá por separado en el componente
      activeAgentsCount: 5,
      fixedVulnerabilitiesCount: 23,
    };
    return of(mockKPIs);
  }

  getCriticalVulnerabilities(): Observable<CriticalVulnerability[]> {
    // Moquear 10 vulnerabilidades críticas
    const mockVulnerabilities: CriticalVulnerability[] = [
      {
        id: '1',
        cve: 'CVE-2023-12345',
        description: 'Buffer overflow in SSH daemon',
        severity: 'Critical',
        targetSystem: 'Server-001',
        reportedDate: new Date('2023-10-01'),
        status: 'Reported',
      },
      {
        id: '2',
        cve: 'CVE-2023-12346',
        description: 'Privilege escalation in kernel module',
        severity: 'Critical',
        targetSystem: 'Server-002',
        reportedDate: new Date('2023-10-02'),
        status: 'Reported',
      },
      {
        id: '3',
        cve: 'CVE-2023-12347',
        description: 'SQL injection in web application',
        severity: 'Critical',
        targetSystem: 'Web-001',
        reportedDate: new Date('2023-10-03'),
        status: 'Reported',
      },
      {
        id: '4',
        cve: 'CVE-2023-12348',
        description: 'Remote code execution in API',
        severity: 'Critical',
        targetSystem: 'API-001',
        reportedDate: new Date('2023-10-04'),
        status: 'Reported',
      },
      {
        id: '5',
        cve: 'CVE-2023-12349',
        description: 'Denial of service in network service',
        severity: 'Critical',
        targetSystem: 'Server-003',
        reportedDate: new Date('2023-10-05'),
        status: 'Reported',
      },
      {
        id: '6',
        cve: 'CVE-2023-12350',
        description: 'Data leakage in database',
        severity: 'Critical',
        targetSystem: 'DB-001',
        reportedDate: new Date('2023-10-06'),
        status: 'Reported',
      },
      {
        id: '7',
        cve: 'CVE-2023-12351',
        description: 'Authentication bypass',
        severity: 'Critical',
        targetSystem: 'Auth-001',
        reportedDate: new Date('2023-10-07'),
        status: 'Reported',
      },
      {
        id: '8',
        cve: 'CVE-2023-12352',
        description: 'Cross-site scripting in frontend',
        severity: 'Critical',
        targetSystem: 'Web-002',
        reportedDate: new Date('2023-10-08'),
        status: 'Reported',
      },
      {
        id: '9',
        cve: 'CVE-2023-12353',
        description: 'Man-in-the-middle vulnerability',
        severity: 'Critical',
        targetSystem: 'Server-004',
        reportedDate: new Date('2023-10-09'),
        status: 'Reported',
      },
      {
        id: '10',
        cve: 'CVE-2023-12354',
        description: 'Zero-day exploit in third-party library',
        severity: 'Critical',
        targetSystem: 'App-001',
        reportedDate: new Date('2023-10-10'),
        status: 'Reported',
      },
    ];
    return of(mockVulnerabilities);
  }

  getVulnerabilitiesChartData(): Observable<ChartData> {
    const mockData: ChartData = {
      labels: ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio'],
      datasets: [
        {
          label: 'Vulnerabilidades Detectadas',
          data: [12, 19, 3, 5, 2, 3],
          backgroundColor: [
            'rgba(255, 99, 132, 0.2)',
            'rgba(54, 162, 235, 0.2)',
            'rgba(255, 206, 86, 0.2)',
            'rgba(75, 192, 192, 0.2)',
            'rgba(153, 102, 255, 0.2)',
            'rgba(255, 159, 64, 0.2)'
          ],
          borderColor: 'rgba(255, 99, 132, 1)',
          fill: false
        }
      ]
    };
    return of(mockData);
  }
}
