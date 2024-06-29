import { Routes } from '@angular/router';

export const UsersRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/users.component').then(x => x.UsersComponent),
  },
  {
    path: 'create',
    loadComponent: () =>
      import('./feature/create-user/create-user.component').then(
        x => x.CreateUserComponent
      ),
  },
  {
    path: 'edit/:id',
    loadComponent: () =>
      import('./feature/edit-user/edit-user.component').then(
        x => x.EditUserComponent
      ),
  },
];
