export interface Agent {
  id: string;
  name: string;
  status: 'active' | 'inactive';
  version: string;
  lastConnection: Date;
}

export interface AgentMetrics {
  activeAgents: number;
  detectedVulnerabilities: number;
  appliedRemediations: number;
  averageUptime: number;
  vulnerabilitiesOverTime: VulnerabilityDataPoint[];
}

export interface VulnerabilityDataPoint {
  week: string;
  count: number;
}
