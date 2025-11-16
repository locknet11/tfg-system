export interface AlertConfiguration {
  id: string;
  sendTo: string;
  conditions: WhenCondition[];
  enabled: boolean;
  lastTriggeredAt?: string;
  createdAt: string;
  updatedAt: string;
}

export enum WhenCondition {
  ON_VULNERABILITY_DETECTED = 'ON_VULNERABILITY_DETECTED',
  ON_HIGH_SEVERITY_VULNERABILITY = 'ON_HIGH_SEVERITY_VULNERABILITY',
  ON_REMEDIATION_SUCCESS = 'ON_REMEDIATION_SUCCESS',
  ON_REMEDIATION_FAILURE = 'ON_REMEDIATION_FAILURE',
  ON_SCAN_COMPLETED = 'ON_SCAN_COMPLETED',
}

export function whenConditionLabel(condition: WhenCondition): string {
  switch (condition) {
    case WhenCondition.ON_VULNERABILITY_DETECTED:
      return $localize`Vulnerability detected`;
    case WhenCondition.ON_HIGH_SEVERITY_VULNERABILITY:
      return $localize`High severity vulnerability`;
    case WhenCondition.ON_REMEDIATION_SUCCESS:
      return $localize`Remediation success`;
    case WhenCondition.ON_REMEDIATION_FAILURE:
      return $localize`Remediation failure`;
    case WhenCondition.ON_SCAN_COMPLETED:
      return $localize`Scan completed`;
    default:
      return condition;
  }
}

export function getAllWhenConditions(): { label: string; value: WhenCondition }[] {
  return Object.values(WhenCondition).map(condition => ({
    label: whenConditionLabel(condition),
    value: condition,
  }));
}
