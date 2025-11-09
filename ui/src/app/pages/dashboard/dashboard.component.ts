import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ChartModule } from 'primeng/chart';
import { BadgeModule } from 'primeng/badge';
import { DashboardService } from './data-access/dashboard.service';
import { TargetsService } from '../targets/data-access/targets.service';
import {
  DashboardKPIs,
  CriticalVulnerability,
  ChartData,
} from './data-access/dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, CardModule, TableModule, ChartModule, BadgeModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  providers: [DashboardService, TargetsService],
})
export class DashboardComponent implements OnInit {
  kpis: DashboardKPIs | null = null;
  vulnerabilities: CriticalVulnerability[] = [];
  chartData: ChartData | null = null;
  chartOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top',
      },
    },
  };

  constructor(
    private dashboardService: DashboardService,
    private targetsService: TargetsService
  ) {}

  ngOnInit(): void {
    // Obtener KPIs moqueados
    this.dashboardService.getKPIs().subscribe(kpis => {
      this.kpis = kpis;
      // Obtener cantidad de targets y actualizar
      this.targetsService.getTargets(0, 1000).subscribe(targetsList => {
        if (this.kpis) {
          this.kpis.targetsCount = targetsList.totalElements;
        }
      });
    });

    this.dashboardService.getCriticalVulnerabilities().subscribe(vulns => {
      this.vulnerabilities = vulns;
    });

    this.dashboardService.getVulnerabilitiesChartData().subscribe(data => {
      this.chartData = data;
    });
  }
}
