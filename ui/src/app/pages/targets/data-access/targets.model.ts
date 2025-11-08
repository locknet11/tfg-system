export interface Target {
  id: string;
  systemName: string;
  ipOrDomain: string;
  status: 'online' | 'offline' | 'in_review';
  assignedAgent: string;
}

export enum TargetStatus {
  ONLINE = 'online',
  OFFLINE = 'offline',
  IN_REVIEW = 'in_review',
}
