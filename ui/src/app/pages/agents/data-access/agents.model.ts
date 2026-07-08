export enum AgentStatus {
  IN_CREATION = 'IN_CREATION',
  CREATED = 'CREATED',
  ACTIVE = 'ACTIVE',
  UNRESPONSIVE = 'UNRESPONSIVE',
  KILLED = 'KILLED',
}

export interface Agent {
  id: string;
  name: string;
  status: AgentStatus;
  version: string;
  lastConnection: Date;
  plan?: Plan;
  targetSystem?: string;
  organizationId?: string;
  projectId?: string;
  replicatedFrom?: string;
  replicatedAt?: string;
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

export enum StepAction {
  SYSTEM_SCAN = 'SYSTEM_SCAN',
  SERVICE_SCAN = 'SERVICE_SCAN',
  NETWORK_SCAN = 'NETWORK_SCAN',
  GENERATE_REPORT = 'GENERATE_REPORT',
  SEND_REPORT = 'SEND_REPORT',
  EXPLOITATION_KNOWLEDGE = 'EXPLOITATION_KNOWLEDGE',
  REQUEST_REPLICATION = 'REQUEST_REPLICATION',
  EXECUTE_EXPLOIT = 'EXECUTE_EXPLOIT',
  TRANSFER_AGENT = 'TRANSFER_AGENT',
  REPLICATE = 'REPLICATE',
  SELF_DESTRUCT = 'SELF_DESTRUCT',
}

export enum StepExecutionStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
}

export interface Step {
  status: StepExecutionStatus;
  action: StepAction;
  logs: string[];
}

export interface Plan {
  notes?: string;
  allowTemplating: boolean;
  steps: Step[];
}

export interface AssignPlanRequest {
  useTemplate: boolean;
  templateId?: string;
  plan?: Plan;
}

export interface AgentPlanInfo {
  notes?: string;
  allowTemplating: boolean;
  targetId?: string;
  targetIp?: string;
  steps: Step[];
}

export interface AgentPlatformInfo {
  platform: string;
  label: string;
  agentVersion: string;
  fileSizeBytes: number;
  blake3Hash: string;
  lastBuilt: string;
}
