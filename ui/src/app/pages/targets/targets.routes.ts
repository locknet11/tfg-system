import { Routes } from '@angular/router';

export const TargetsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/targets.component').then(x => x.TargetsComponent),
  },
];