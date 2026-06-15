import { Routes } from '@angular/router';

export const vulnerabilitiesRoutes: Routes = [
  {
    path: '',
    title: $localize`Vulnerabilities`,
    loadComponent: () =>
      import('./feature/vulnerabilities.component').then(
        m => m.VulnerabilitiesComponent
      ),
  },
  {
    path: ':serviceKey',
    title: $localize`Vulnerability Detail`,
    loadComponent: () =>
      import(
        './feature/vulnerability-detail/vulnerability-detail.component'
      ).then(m => m.VulnerabilityDetailComponent),
  },
];
