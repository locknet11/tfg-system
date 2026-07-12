import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageModule } from 'primeng/message';
import { ButtonModule } from 'primeng/button';
import { AgentsService } from '../../data-access/agents.service';
import { AgentMetrics } from '../../data-access/agents.model';

@Component({
  selector: 'app-agents-metrics',
  standalone: true,
  imports: [
    CommonModule,
    CardModule,
    ChartModule,
    SkeletonModule,
    MessageModule,
    ButtonModule,
  ],
  templateUrl: './agents-metrics.component.html',
  styleUrls: ['./agents-metrics.component.scss'],
})
export class AgentsMetricsComponent implements OnInit {
  private agentsService = inject(AgentsService);
  private destroyRef = inject(DestroyRef);

  metrics = signal<AgentMetrics | null>(null);
  loading = signal(true);
  error = signal(false);

  chartData: any = null;
  chartOptions: any;

  ngOnInit() {
    this.loadMetrics();
  }

  loadMetrics(): void {
    this.loading.set(true);
    this.error.set(false);
    this.agentsService
      .getMetrics()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (metrics) => {
          this.metrics.set(metrics);
          this.buildChart(metrics);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.error.set(true);
        },
      });
  }

  buildChart(metrics: AgentMetrics): void {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--text-color');
    const textColorSecondary = documentStyle.getPropertyValue(
      '--text-color-secondary'
    );
    const surfaceBorder = documentStyle.getPropertyValue('--surface-border');

    this.chartData = {
      labels: metrics.vulnerabilityTrend.map((v) => v.period),
      datasets: [
        {
          label: $localize`Vulnerabilities`,
          data: metrics.vulnerabilityTrend.map((v) => v.count),
          fill: true,
          borderColor: '#3b82f6',
          backgroundColor: 'rgba(59, 130, 246, 0.1)',
          tension: 0.5,
          borderWidth: 3,
        },
      ],
    };

    this.chartOptions = {
      maintainAspectRatio: false,
      responsive: true,
      plugins: {
        legend: {
          display: false,
        },
      },
      scales: {
        x: {
          ticks: {
            color: textColorSecondary,
          },
          grid: {
            color: surfaceBorder,
            display: false,
          },
        },
        y: {
          ticks: {
            color: textColorSecondary,
          },
          grid: {
            color: surfaceBorder,
          },
        },
      },
    };
  }
}
