import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService, MessageService } from 'primeng/api';

import {
  AlertConfiguration,
  whenConditionLabel,
} from '../../data-access/alerts.model';
import { AlertsService } from '../../data-access/alerts.service';
import { CreateAlertModalComponent } from '../modals/create-alert-modal/create-alert-modal.component';
import { EditAlertModalComponent } from '../modals/edit-alert-modal/edit-alert-modal.component';

@Component({
  selector: 'app-alerts-list',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    ConfirmDialogModule,
    ToastModule,
    TagModule,
    TooltipModule,
    CreateAlertModalComponent,
    EditAlertModalComponent,
  ],
  templateUrl: './alerts-list.component.html',
  styleUrls: ['./alerts-list.component.scss'],
  providers: [ConfirmationService, MessageService],
})
export class AlertsListComponent {
  private alertsService = inject(AlertsService);
  private confirm = inject(ConfirmationService);
  private messages = inject(MessageService);

  createAlertModal = viewChild.required(CreateAlertModalComponent);
  editAlertModal = viewChild.required(EditAlertModalComponent);

  alertsSig = signal<AlertConfiguration[]>([]);
  totalSig = signal(0);
  loadingSig = signal(false);
  pageSig = signal(0);
  sizeSig = signal(10);

  filteredAlerts = computed(() => this.alertsSig());

  whenConditionLabel = whenConditionLabel;

  ngOnInit() {
    // Lazy loading will trigger initial load
  }

  onLazyLoad(event: any) {
    const page = event.first / event.rows;
    this.pageSig.set(page);
    this.sizeSig.set(event.rows);
    this.loadAlerts();
  }

  editAlert(alert: AlertConfiguration) {
    this.editAlertModal().show(alert);
  }

  createAlert() {
    this.createAlertModal().show();
  }

  deleteAlert(alert: AlertConfiguration) {
    this.confirm.confirm({
      header: $localize`Delete Alert`,
      message: $localize`Are you sure you want to delete this alert configuration?`,
      acceptLabel: $localize`Delete`,
      rejectLabel: $localize`Cancel`,
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.alertsService.delete(alert.id).subscribe({
          next: () => {
            this.messages.add({
              severity: 'success',
              summary: $localize`Success`,
              detail: $localize`Alert deleted`,
            });
            this.loadAlerts();
          },
          error: () =>
            this.messages.add({
              severity: 'error',
              summary: $localize`Error`,
              detail: $localize`Failed to delete alert`,
            }),
        });
      },
    });
  }



  loadAlerts() {
    this.loadingSig.set(true);
    this.alertsService.list(this.pageSig(), this.sizeSig()).subscribe({
      next: res => {
        this.alertsSig.set(res.content);
        this.totalSig.set(res.totalElements);
        this.loadingSig.set(false);
      },
      error: () => this.loadingSig.set(false),
    });
  }
}
