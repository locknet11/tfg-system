import { Component } from '@angular/core';

export const GLOBAL_TOAST = 'global_toast_key';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  toastKey = GLOBAL_TOAST;
  title = 'Project';
}
