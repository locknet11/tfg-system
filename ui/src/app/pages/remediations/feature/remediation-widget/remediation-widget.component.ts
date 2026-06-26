import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { RemediationsService } from '../../data-access/remediations.service';
import { RemediationStatistics } from '../../data-access/remediations.model';

@Component({
  selector: 'app-remediation-widget',
  standalone: true,
  imports: [CommonModule, CardModule, ChartModule],
  templateUrl: './remediation-widget.component.html',
  styleUrl: './remediation-widget.component.scss',
})
export class RemediationWidgetComponent implements OnInit {
  statistics: RemediationStatistics | null = null;
  chartData: any = null;
  chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
      },
    },
  };

  constructor(private remediationsService: RemediationsService) {}

  ngOnInit(): void {
    this.loadStatistics();
  }

  loadStatistics(): void {
    this.remediationsService.getStatistics().subscribe({
      next: stats => {
        this.statistics = stats;
        this.buildChart(stats);
      },
      error: err => {
        console.error('Error loading remediation statistics', err);
      },
    });
  }

  buildChart(stats: RemediationStatistics): void {
    this.chartData = {
      labels: ['Success', 'Failed', 'Pending', 'Pending Reboot', 'Skipped'],
      datasets: [
        {
          data: [
            stats.successCount,
            stats.failedCount,
            stats.pendingCount,
            stats.pendingRebootCount,
            stats.skippedCount,
          ],
          backgroundColor: [
            '#4CAF50', // success - green
            '#F44336', // failed - red
            '#FF9800', // pending - orange
            '#2196F3', // pending reboot - blue
            '#9E9E9E', // skipped - gray
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
    if (!this.statistics || this.statistics.totalRemediations === 0) {
      return 0;
    }
    return Math.round(
      (this.statistics.successCount / this.statistics.totalRemediations) * 100
    );
  }
}
