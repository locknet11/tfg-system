import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { LocalStorageService } from 'src/app/shared/services/local-storage.service';
import { environment } from 'src/environments/environment';

export interface AuthenticationRequest {
  username: string;
  password: string;
}

export interface AuthenticationResponse {
  token: string;
}

export interface SetupStatus {
  needsSetup: boolean;
}

export interface SetupRequest {
  fullName: string;
  email: string;
  password: string;
}

const baseUrl = environment.baseUrl;

@Injectable({
  providedIn: 'root',
})
export class AuthenticationService {
  constructor(
    private http: HttpClient,
    private localStorageService: LocalStorageService
  ) {}
  login(request: AuthenticationRequest) {
    return this.http.post<AuthenticationResponse>(
      `${baseUrl}/auth/login`,
      request
    );
  }

  checkSetupStatus() {
    return this.http.get<SetupStatus>(`${baseUrl}/auth/check-setup`);
  }

  setup(request: SetupRequest) {
    return this.http.post<AuthenticationResponse>(
      `${baseUrl}/auth/setup`,
      request
    );
  }

  saveToken(token: string) {
    this.localStorageService.setToken(token);
  }
}
