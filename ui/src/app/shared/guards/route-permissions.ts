import { ModuleAccess } from '../models/global.model';

export const routePermissions: { [key in ModuleAccess]: string[] } = {
  SETTINGS: []
};
