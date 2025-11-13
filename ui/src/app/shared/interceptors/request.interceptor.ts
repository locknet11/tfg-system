import {
  HttpContextToken,
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpStatusCode,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError } from 'rxjs';
import { AccountService } from '../services/account.service';
import { LocalStorageService } from '../services/local-storage.service';

export const MANAGE_ERRORS = new HttpContextToken<boolean>(() => true);

@Injectable()
export class RequestInterceptor implements HttpInterceptor {
  constructor(
    private localStorageService: LocalStorageService,
    private accountService: AccountService,
    private router: Router
  ) {}
  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    // get token and insert in header
    const token = this.localStorageService.getToken();
    const selectedOrganization = this.localStorageService.getSelectedOrganization();
    const selectedProject = this.localStorageService.getSelectedProject();

    const headers: any = {};
    if (token) {
      headers.Authorization = token;
    }
    if (selectedOrganization?.id) {
      headers['X-Organization-Id'] = selectedOrganization.id;
    }
    if (selectedProject?.id) {
      headers['X-Project-Id'] = selectedProject.id;
    }

    if (Object.keys(headers).length > 0) {
      req = req.clone({ setHeaders: headers });
    }

    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (req.context.get(MANAGE_ERRORS) === true) {
          this.manageErrors(error);
        }
        throw error;
      })
    );
  }

  manageErrors(error: HttpErrorResponse) {
    if (error.status === HttpStatusCode.Unauthorized) {
      this.accountService.logout();
      this.router.navigate(['login']);
    }
  }
}
