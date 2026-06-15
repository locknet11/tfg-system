import { Routes } from '@angular/router';

export const TargetsRoutes: Routes = [
  {
    path: '',
    title: $localize`Targets`,
    loadComponent: () =>
      import('./feature/targets.component').then(x => x.TargetsComponent),
  },
];
