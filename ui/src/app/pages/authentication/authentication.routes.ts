import { Routes } from '@angular/router';
import { needsAuthentication } from 'src/app/shared/guards/needs-authentication.guard';
import { needsSetup } from 'src/app/shared/guards/needs-setup.guard';

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
        canMatch: [needsSetup, needsAuthentication],
        loadComponent: () =>
          import('./feature/authentication.component').then(
            x => x.AuthenticationComponent
          ),
      },
      {
        path: 'initial-setup',
        canMatch: [needsSetup],
        loadComponent: () =>
          import('./feature/initial-setup/initial-setup.component').then(
            x => x.InitialSetupComponent
          ),
      },
      // 2fa path
    ],
  },
];
