export interface DashboardKPIs {
  targetsCount: number;
  activeAgentsCount: number;
  fixedVulnerabilitiesCount: number;
}

export interface CriticalVulnerability {
  id: string;
  cve: string;
  description: string;
  severity: 'Critical' | 'High' | 'Medium' | 'Low';
  targetSystem: string;
  reportedDate: Date;
  status: 'Reported' | 'In Progress' | 'Fixed';
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