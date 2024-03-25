import { Routes } from '@angular/router';
import { needsAuthentication } from 'src/app/shared/guards/needs-authentication.guard';

export const AuthenticationRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./ui/authentication-layout.component').then(
        x => x.AuthenticationLayoutComponent
      ),
    children: [
      {
        path: '',
        canMatch: [needsAuthentication],
        loadComponent: () =>
          import('./feature/authentication.component').then(
            x => x.AuthenticationComponent
          ),
      },
      // 2fa path
    ],
  },
];
