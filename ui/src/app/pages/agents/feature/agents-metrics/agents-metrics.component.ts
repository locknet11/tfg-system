import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { AgentMetrics } from '../../data-access/agents.model';

@Component({
  selector: 'app-agents-metrics',
  standalone: true,
  imports: [CommonModule, CardModule, ChartModule],
  templateUrl: './agents-metrics.component.html',
  styleUrls: ['./agents-metrics.component.scss'],
})
export class AgentsMetricsComponent implements OnInit {
  metrics: AgentMetrics = {
    activeAgents: 125,
    detectedVulnerabilities: 348,
    appliedRemediations: 212,
    averageUptime: 99.8,
    vulnerabilitiesOverTime: [
      { week: 'Sem 1', count: 85 },
      { week: 'Sem 2', count: 120 },
      { week: 'Sem 3', count: 95 },
      { week: 'Sem 4', count: 110 },
    ],
  };

  chartData: any;
  chartOptions: any;

  ngOnInit() {
    this.initChart();
  }

  initChart() {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--text-color');
    const textColorSecondary = documentStyle.getPropertyValue(
      '--text-color-secondary'
    );
    const surfaceBorder = documentStyle.getPropertyValue('--surface-border');

    this.chartData = {
      labels: this.metrics.vulnerabilitiesOverTime.map(v => v.week),
      datasets: [
        {
          label: 'Vulnerabilities',
          data: this.metrics.vulnerabilitiesOverTime.map(v => v.count),
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
