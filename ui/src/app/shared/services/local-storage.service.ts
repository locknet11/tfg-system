import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LocalStorageService {
  TOKEN: string = 'token';

  setToken(token: string) {
    localStorage.setItem(this.TOKEN, token);
  }
  getToken() {
    return localStorage.getItem(this.TOKEN);
  }
  removeToken() {
    localStorage.removeItem(this.TOKEN);
  }
}
