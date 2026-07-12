import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ChartModule } from 'primeng/chart';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';
import { DashboardService } from './data-access/dashboard.service';
import { RemediationWidgetComponent } from '../remediations/feature/remediation-widget/remediation-widget.component';
import {
  DashboardKPIs,
  CriticalVulnerability,
  ChartData,
} from './data-access/dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    CardModule,
    TableModule,
    ChartModule,
    BadgeModule,
    ButtonModule,
    MessageModule,
    SkeletonModule,
    RemediationWidgetComponent,
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  providers: [DashboardService],
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private destroyRef = inject(DestroyRef);

  kpis = signal<DashboardKPIs | null>(null);
  vulnerabilities = signal<CriticalVulnerability[]>([]);
  chartData = signal<ChartData | null>(null);

  kpisLoading = signal(true);
  vulnsLoading = signal(true);
  chartLoading = signal(true);

  kpisError = signal(false);
  vulnsError = signal(false);
  chartError = signal(false);

  readonly chartOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top' as const,
      },
    },
  };

  ngOnInit(): void {
    this.loadKpis();
    this.loadCriticalVulnerabilities();
    this.loadVulnerabilityChart();
  }

  loadKpis(): void {
    this.kpisLoading.set(true);
    this.kpisError.set(false);
    this.dashboardService
      .getKPIs()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (kpis) => {
          this.kpis.set(kpis);
          this.kpisLoading.set(false);
        },
        error: () => {
          this.kpisLoading.set(false);
          this.kpisError.set(true);
        },
      });
  }

  loadCriticalVulnerabilities(): void {
    this.vulnsLoading.set(true);
    this.vulnsError.set(false);
    this.dashboardService
      .getCriticalVulnerabilities()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (vulns) => {
          this.vulnerabilities.set(vulns);
          this.vulnsLoading.set(false);
        },
        error: () => {
          this.vulnsLoading.set(false);
          this.vulnsError.set(true);
        },
      });
  }

  loadVulnerabilityChart(): void {
    this.chartLoading.set(true);
    this.chartError.set(false);
    this.dashboardService
      .getVulnerabilitiesChartData()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.chartData.set(data);
          this.chartLoading.set(false);
        },
        error: () => {
          this.chartLoading.set(false);
          this.chartError.set(true);
        },
      });
  }
}
