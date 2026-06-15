import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { LocalStorageService } from './local-storage.service';

@Injectable({ providedIn: 'root' })
export class CustomTitleStrategy extends TitleStrategy {
  constructor(
    private readonly title: Title,
    private readonly localStorageService: LocalStorageService,
  ) {
    super();
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const routeTitle = this.buildTitle(snapshot);
    const defaultTitle = 'CloudGuard';

    const title = routeTitle ?? defaultTitle;

    const org = this.localStorageService.getSelectedOrganization();
    const proj = this.localStorageService.getSelectedProject();

    if (org?.name && proj?.name) {
      this.title.setTitle(`${title} - ${org.name}/${proj.name} - ${defaultTitle}`);
    } else {
      this.title.setTitle(`${title} - ${defaultTitle}`);
    }
  }
}
