import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { ToastService } from 'src/app/shared/services/toast.service';
import { RemediationStatus } from '../../../remediations/data-access/remediations.model';
import { Report } from '../../data-access/reports.model';
import { ReportsService } from '../../data-access/reports.service';

interface ChartConfig {
  readonly labels: string[];
  readonly datasets: {
    data: number[];
    backgroundColor: string[];
  }[];
}

const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: '#B71C1C',
  HIGH: '#F44336',
  MEDIUM: '#FF9800',
  LOW: '#FBC02D',
  UNKNOWN: '#9E9E9E',
};

const STATUS_COLORS: Record<string, string> = {
  SUCCESS: '#4CAF50',
  FAILED: '#F44336',
  PENDING: '#FF9800',
  IN_PROGRESS: '#2196F3',
  PENDING_REBOOT: '#3F51B5',
  SKIPPED: '#9E9E9E',
};

@Component({
  selector: 'app-report-detail',
  standalone: true,
  imports: [
    CommonModule,
    CardModule,
    ChartModule,
    TableModule,
    TagModule,
    ButtonModule,
  ],
  templateUrl: './report-detail.component.html',
  styleUrls: ['./report-detail.component.scss'],
})
export class ReportDetailComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly reportsService = inject(ReportsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  readonly report = signal<Report | null>(null);
  readonly loadingSig = signal(false);
  readonly severityChart = signal<ChartConfig | null>(null);
  readonly statusChart = signal<ChartConfig | null>(null);

  readonly chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
      },
    },
  };

  backTooltip = $localize`Back to reports`;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/reports']);
      return;
    }
    this.loadReport(id);
  }

  back(): void {
    this.router.navigate(['/reports']);
  }

  mttrLabel(): string {
    const report = this.report();
    if (!report) return '-';
    const seconds = report.summary.meanTimeToRemediateSeconds;
    if (seconds <= 0) return '-';
    const minutes = Math.floor(seconds / 60);
    const remaining = seconds % 60;
    return minutes > 0 ? `${minutes}m ${remaining}s` : `${remaining}s`;
  }

  severityLabel(severity: string | null): string {
    switch (severity) {
      case 'CRITICAL':
        return $localize`Critical`;
      case 'HIGH':
        return $localize`High`;
      case 'MEDIUM':
        return $localize`Medium`;
      case 'LOW':
        return $localize`Low`;
      case 'UNKNOWN':
        return $localize`Unknown`;
      default:
        return severity ?? '';
    }
  }

  remediationStatusLabel(status: RemediationStatus): string {
    switch (status) {
      case 'SUCCESS':
        return $localize`Success`;
      case 'FAILED':
        return $localize`Failed`;
      case 'PENDING':
        return $localize`Pending`;
      case 'IN_PROGRESS':
        return $localize`In Progress`;
      case 'SKIPPED':
        return $localize`Skipped`;
      case 'PENDING_REBOOT':
        return $localize`Pending Reboot`;
      default:
        return status;
    }
  }

  getStatusSeverity(
    status: RemediationStatus
  ): 'success' | 'danger' | 'warning' | 'info' | undefined {
    switch (status) {
      case 'SUCCESS':
        return 'success';
      case 'FAILED':
        return 'danger';
      case 'PENDING':
      case 'IN_PROGRESS':
        return 'warning';
      case 'SKIPPED':
      case 'PENDING_REBOOT':
        return 'info';
      default:
        return undefined;
    }
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString();
  }

  private loadReport(id: string): void {
    this.loadingSig.set(true);
    this.reportsService
      .get(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: report => {
          this.report.set(report);
          this.buildCharts(report);
          this.loadingSig.set(false);
        },
        error: () => {
          this.loadingSig.set(false);
          this.toastService.error($localize`Failed to load report.`);
          this.router.navigate(['/reports']);
        },
      });
  }

  private buildCharts(report: Report): void {
    this.severityChart.set(
      this.toChart(report.summary.vulnerabilitiesBySeverity, SEVERITY_COLORS)
    );
    this.statusChart.set(
      this.toChart(report.summary.remediationsByStatus, STATUS_COLORS)
    );
  }

  private translateChartKey(
    key: string,
    colors: Record<string, string>
  ): string {
    const isSeverityChart = 'CRITICAL' in colors;
    if (isSeverityChart) {
      return this.severityLabel(key);
    }
    return this.remediationStatusLabel(key as RemediationStatus);
  }

  private toChart(
    counts: Readonly<Record<string, number>>,
    colors: Record<string, string>
  ): ChartConfig {
    const labels: string[] = [];
    const data: number[] = [];
    const backgroundColor: string[] = [];

    Object.entries(counts).forEach(([key, value]) => {
      if (value > 0) {
        labels.push(this.translateChartKey(key, colors));
        data.push(value);
        backgroundColor.push(colors[key] ?? '#9E9E9E');
      }
    });

    return { labels, datasets: [{ data, backgroundColor }] };
  }
}
