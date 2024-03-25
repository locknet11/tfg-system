export interface AccountInfo {
  id: string;
  email: string;
  fullName: string;
  role: string;
  moduleAccess: ModuleAccess[];
}

export type ModuleAccess = 'SETTINGS';
