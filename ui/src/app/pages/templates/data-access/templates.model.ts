export interface PlanTemplate {
  id: string;
  name: string;
  description: string;
  plan: Plan;
  createdAt: string;
  updatedAt: string;
}

export interface Plan {
  notes: string;
  steps: Step[];
}

export interface Step {
  status?: StepExecutionStatus;
  action: StepAction;
  logs?: string[];
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
}

export enum StepExecutionStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
}

export function stepActionLabel(action: StepAction): string {
  switch (action) {
    case StepAction.SYSTEM_SCAN:
      return $localize`System scan`;
    case StepAction.SERVICE_SCAN:
      return $localize`Service scan`;
    case StepAction.NETWORK_SCAN:
      return $localize`Network scan`;
    case StepAction.GENERATE_REPORT:
      return $localize`Generate report`;
    case StepAction.SEND_REPORT:
      return $localize`Send report`;
    case StepAction.EXPLOITATION_KNOWLEDGE:
      return $localize`Exploitation knowledge`;
    case StepAction.REQUEST_REPLICATION:
      return $localize`Request replication`;
    case StepAction.EXECUTE_EXPLOIT:
      return $localize`Execute exploit`;
    case StepAction.TRANSFER_AGENT:
      return $localize`Transfer agent`;
    case StepAction.REPLICATE:
      return $localize`Replicate`;
    default:
      return action;
  }
}
