import { Routes } from '@angular/router';

export const UsersRoutes: Routes = [
  {
    path: '',
    title: $localize`Users`,
    loadComponent: () =>
      import('./feature/users.component').then(x => x.UsersComponent),
  },
  {
    path: 'create',
    title: $localize`New User`,
    loadComponent: () =>
      import('./feature/create-user/create-user.component').then(
        x => x.CreateUserComponent
      ),
  },
  {
    path: 'edit/:id',
    title: $localize`Edit User`,
    loadComponent: () =>
      import('./feature/edit-user/edit-user.component').then(
        x => x.EditUserComponent
      ),
  },
];
