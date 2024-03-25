import { Injectable } from '@angular/core';
import {
  BehaviorSubject,
  Observable,
  firstValueFrom,
  lastValueFrom,
  shareReplay,
  switchMap,
} from 'rxjs';
import { AccountInfo } from '../models/global.model';
import { environment } from 'src/environments/environment';
import { HttpClient } from '@angular/common/http';
import { LocalStorageService } from './local-storage.service';
import { Router } from '@angular/router';

const baseUrl = environment.baseUrl;

@Injectable({
  providedIn: 'root',
})
export class AccountService {
  constructor(
    private http: HttpClient,
    private localStorageService: LocalStorageService,
    private router: Router
  ) {}

  private _refreshAccountData = new BehaviorSubject<any>(true);
  $accountData: Observable<AccountInfo> = this._refreshAccountData.pipe(
    switchMap(() => this.getAccountInfo()),
    shareReplay(1)
  );

  private getAccountInfo() {
    return this.http.post<AccountInfo>(
      `${baseUrl}/auth/account-info`,
      undefined
    );
  }

  refreshAccountInfo() {
    this._refreshAccountData.next(true);
  }

  logout() {
    this.localStorageService.removeToken();
  }
}
