import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { AuthenticationService } from 'src/app/pages/authentication/data-access/authentication.service';
import { lastValueFrom } from 'rxjs';

export const needsSetup: CanMatchFn = async (route, segments) => {
  const router = inject(Router);
  const authService = inject(AuthenticationService);
  const setupStatus = await lastValueFrom(authService.checkSetupStatus());
  const isInitialSetupRoute = segments.some(s => s.path === 'initial-setup');

  if (setupStatus.needsSetup) {
    if (isInitialSetupRoute) {
      return true;
    }
    router.navigateByUrl('login/initial-setup');
    return false;
  }

  if (isInitialSetupRoute) {
    router.navigateByUrl('login');
    return false;
  }
  return true;
};
