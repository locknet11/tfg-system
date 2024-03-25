import { Routes } from '@angular/router';

export const UsersRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/users.component').then(x => x.UsersComponent),
  },
];
