export enum AgentStatus {
  IN_CREATION = 'IN_CREATION',
  CREATED = 'CREATED',
  ACTIVE = 'ACTIVE',
  UNRESPONSIVE = 'UNRESPONSIVE',
  KILLED = 'KILLED'
}

export interface Agent {
  id: string;
  name: string;
  status: AgentStatus;
  version: string;
  lastConnection: Date;
  targetSystem?: string;
}

export interface AgentsList {
  content: Agent[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
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
