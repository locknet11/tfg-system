import { Routes } from '@angular/router';

export const reportsRoutes: Routes = [
  {
    path: '',
    title: $localize`Reports`,
    loadComponent: () =>
      import('./feature/reports-list/reports-list.component').then(
        m => m.ReportsListComponent
      ),
  },
  {
    path: ':id',
    title: $localize`Report Detail`,
    loadComponent: () =>
      import('./feature/report-detail/report-detail.component').then(
        m => m.ReportDetailComponent
      ),
  },
];
