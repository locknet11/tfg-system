import { Routes } from '@angular/router';

export const templatesRoutes: Routes = [
  {
    path: '',
    title: 'Templates',
    loadComponent: () =>
      import('./feature/templates-list/templates-list.component').then(
        m => m.TemplatesListComponent
      ),
  },
];
