import { Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';
import { GLOBAL_TOAST } from 'src/app/app.component';

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  constructor(private messageService: MessageService) {}

  error(detail: string) {
    this.messageService.add({
      detail: detail,
      severity: 'error',
      key: GLOBAL_TOAST,
    });
  }
  info(detail: string) {
    this.messageService.add({
      detail: detail,
      severity: 'info',
      key: GLOBAL_TOAST,
    });
  }
  success(detail: string) {
    this.messageService.add({
      detail: detail,
      severity: 'success',
      key: GLOBAL_TOAST,
    });
  }
}
