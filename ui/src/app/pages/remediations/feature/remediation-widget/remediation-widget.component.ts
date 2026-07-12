import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageModule } from 'primeng/message';
import { ButtonModule } from 'primeng/button';
import { RemediationsService } from '../../data-access/remediations.service';
import { RemediationStatistics } from '../../data-access/remediations.model';

@Component({
  selector: 'app-remediation-widget',
  standalone: true,
  imports: [
    CommonModule,
    CardModule,
    ChartModule,
    SkeletonModule,
    MessageModule,
    ButtonModule,
  ],
  templateUrl: './remediation-widget.component.html',
  styleUrl: './remediation-widget.component.scss',
})
export class RemediationWidgetComponent implements OnInit {
  statistics = signal<RemediationStatistics | null>(null);
  chartData: any = null;
  loading = signal(true);
  error = signal(false);

  chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom' as const,
      },
    },
  };

  constructor(private remediationsService: RemediationsService) {}

  ngOnInit(): void {
    this.loadStatistics();
  }

  loadStatistics(): void {
    this.loading.set(true);
    this.error.set(false);
    this.remediationsService.getStatistics().subscribe({
      next: (stats) => {
        this.statistics.set(stats);
        this.buildChart(stats);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  buildChart(stats: RemediationStatistics): void {
    this.chartData = {
      labels: [
        $localize`Success`,
        $localize`Failed`,
        $localize`Pending`,
        $localize`Pending Reboot`,
        $localize`Skipped`,
      ],
      datasets: [
        {
          data: [
            this.statusCount(stats, 'SUCCESS'),
            this.statusCount(stats, 'FAILED'),
            this.statusCount(stats, 'PENDING'),
            this.statusCount(stats, 'PENDING_REBOOT'),
            this.statusCount(stats, 'SKIPPED'),
          ],
          backgroundColor: [
            '#4CAF50',
            '#F44336',
            '#FF9800',
            '#2196F3',
            '#9E9E9E',
          ],
          hoverBackgroundColor: [
            '#45a049',
            '#da190b',
            '#e68900',
            '#0d8aee',
            '#757575',
          ],
        },
      ],
    };
  }

  get successRate(): number {
    const stats = this.statistics();
    if (!stats || stats.totalCount === 0) {
      return 0;
    }
    return Math.round(
      (this.statusCount(stats, 'SUCCESS') / stats.totalCount) * 100
    );
  }

  get mttr(): string {
    const seconds = this.statistics()?.meanTimeToRemediateSeconds ?? 0;
    if (seconds <= 0) {
      return $localize`N/A`;
    }
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0 && minutes > 0) {
      return `${hours}h ${minutes}m`;
    } else if (hours > 0) {
      return `${hours}h`;
    } else {
      return `${minutes}m`;
    }
  }

  statusCount(stats: RemediationStatistics, key: string): number {
    return stats.byStatus[key] ?? 0;
  }
}
