export interface DashboardKPIs {
  readonly targetsCount: number;
  readonly activeAgentsCount: number;
  readonly fixedVulnerabilitiesCount: number;
}

export interface CriticalVulnerability {
  readonly serviceKey: string;
  readonly cveId: string;
  readonly description: string;
  readonly serviceName: string;
  readonly cvssScore: number | null;
  readonly reportedDate: string;
}

export interface VulnerabilityTrendPoint {
  readonly period: string;
  readonly count: number;
}

export interface ChartData {
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    backgroundColor?: string[];
    borderColor?: string;
    fill?: boolean;
  }[];
}
