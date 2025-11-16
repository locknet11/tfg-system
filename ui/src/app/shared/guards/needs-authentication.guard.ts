import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { LocalStorageService } from '../services/local-storage.service';

export const needsAuthentication: CanMatchFn = (_, segments) => {
  const router = inject(Router);
  if (inject(LocalStorageService).getToken()) {
    router.navigateByUrl('dashboard');
    return false;
  }
  return true;
};
