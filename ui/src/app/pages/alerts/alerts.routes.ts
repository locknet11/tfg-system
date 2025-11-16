import { Routes } from '@angular/router';

export const alertsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/alerts-list/alerts-list.component').then(
        m => m.AlertsListComponent
      ),
  },
];
