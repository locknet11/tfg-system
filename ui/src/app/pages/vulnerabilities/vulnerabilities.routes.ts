import { Routes } from '@angular/router';

export const vulnerabilitiesRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/vulnerabilities.component').then(
        m => m.VulnerabilitiesComponent
      ),
  },
  {
    path: ':serviceKey',
    loadComponent: () =>
      import(
        './feature/vulnerability-detail/vulnerability-detail.component'
      ).then(m => m.VulnerabilityDetailComponent),
  },
];
