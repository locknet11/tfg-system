import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LocalStorageService {
  TOKEN: string = 'token';
  SELECTED_ORGANIZATION: string = 'selectedOrganization';
  SELECTED_PROJECT: string = 'selectedProject';

  setToken(token: string) {
    localStorage.setItem(this.TOKEN, token);
  }
  getToken() {
    return localStorage.getItem(this.TOKEN);
  }
  removeToken() {
    localStorage.removeItem(this.TOKEN);
  }

  getSelectedOrganization() {
    const orgStr = localStorage.getItem(this.SELECTED_ORGANIZATION);
    return orgStr ? JSON.parse(orgStr) : null;
  }

  getSelectedProject() {
    const projectStr = localStorage.getItem(this.SELECTED_PROJECT);
    return projectStr ? JSON.parse(projectStr) : null;
  }
}
