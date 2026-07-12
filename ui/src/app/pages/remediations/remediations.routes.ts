import { Routes } from '@angular/router';

export const remediationsRoutes: Routes = [
  {
    path: '',
    title: $localize`Remediations`,
    loadComponent: () =>
      import('./feature/remediations-list/remediations-list.component').then(
        m => m.RemediationsListComponent
      ),
  },
  {
    path: 'strategies',
    title: $localize`Remediation Strategies`,
    loadComponent: () =>
      import('./feature/strategies-list/strategies-list.component').then(
        m => m.StrategiesListComponent
      ),
  },
  {
    path: ':id',
    title: $localize`Remediation Detail`,
    loadComponent: () =>
      import('./feature/remediation-detail/remediation-detail.component').then(
        m => m.RemediationDetailComponent
      ),
  },
];
