import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { LocalStorageService } from '../services/local-storage.service';
import { AccountService } from '../services/account.service';
import { map } from 'rxjs';
import { ToastService } from '../services/toast.service';
import { routePermissions } from './route-permissions';

export const isAuthenticated: CanMatchFn = (_, segments) => {
  const router = inject(Router);
  if (!inject(LocalStorageService).getToken()) {
    router.navigateByUrl('/login');
    return false;
  }
  const fullPath = segments.reduce((path, currentSegment) => {
    return `${path}/${currentSegment.path}`;
  }, '');
  return hasAccess(fullPath);
};

const hasAccess = (fullPath: string) => {
  const router = inject(Router);
  const accountService = inject(AccountService);
  const toastService = inject(ToastService);
  if (fullPath === '/page-not-found') return true;
  return accountService.$accountData.pipe(
    map(accountData => {
      let access: boolean = false;
      if (accountData.role === 'ADMIN') {
        access = true;
      }

      if (accountData.role === 'CLIENT') {
        router.navigateByUrl('login');
        accountService.logout();
        toastService.error($localize`Role not allowed in this site`);
        return false;
      }
      if (accountData.role === 'USER') {
        for (let module of accountData.moduleAccess) {
          const routes: string[] = routePermissions[module];
          if (!routes) continue;
          if (routes.find(r => fullPath.includes(r))) {
            access = true;
            break;
          }
        }
      }
      if (!access) {
        router.navigateByUrl('/page-not-found');
      }
      return access;
    })
  );
};
