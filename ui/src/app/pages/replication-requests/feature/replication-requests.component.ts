import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { DropdownModule } from 'primeng/dropdown';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';

import { ToastService } from 'src/app/shared/services/toast.service';
import {
  ReplicationRequest,
  ReplicationRequestStatus,
} from '../data-access/replication-requests.model';
import { ReplicationRequestsService } from '../data-access/replication-requests.service';

@Component({
  selector: 'app-replication-requests',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    TagModule,
    DropdownModule,
    TooltipModule,
    ConfirmDialogModule,
  ],
  styleUrls: ['./replication-requests.component.scss'],
  providers: [ConfirmationService],
  templateUrl: './replication-requests.component.html',
})
export class ReplicationRequestsComponent {
  private readonly service = inject(ReplicationRequestsService);
  private readonly toastService = inject(ToastService);
  private readonly confirmationService = inject(ConfirmationService);

  requests = signal<ReplicationRequest[]>([]);
  totalRecords = signal(0);
  loading = signal(false);
  page = 0;
  rows = 10;

  readonly emptyMessage = $localize`No replication requests found`;

  statusFilter = signal<string | null>(null);
  severityFilter = signal<string | null>(null);

  readonly statusOptions = Object.values(ReplicationRequestStatus).map(s => ({
    label: s,
    value: s,
  }));

  readonly severityOptions = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map(s => ({
    label: s,
    value: s,
  }));

  loadRequests(event: TableLazyLoadEvent = {}): void {
    this.loading.set(true);
    this.page = Math.floor((event.first ?? 0) / (event.rows ?? this.rows));
    this.rows = event.rows ?? this.rows;

    this.service
      .list(
        this.page,
        this.rows,
        this.statusFilter() ?? undefined,
        this.severityFilter() ?? undefined
      )
      .subscribe({
        next: result => {
          this.requests.set(result.content);
          this.totalRecords.set(result.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.toastService.error(
            $localize`Failed to load replication requests`
          );
          this.loading.set(false);
        },
      });
  }

  onFilterChange(): void {
    this.page = 0;
    this.loadRequests();
  }

  approveRequest(request: ReplicationRequest): void {
    this.confirmationService.confirm({
      header: $localize`Approve Replication`,
      message: $localize`Approve replication to target ${request.targetIp} using ${request.exploitId}?`,
      acceptLabel: $localize`Approve`,
      rejectLabel: $localize`Cancel`,
      accept: () => {
        this.service.approve(request.id).subscribe({
          next: () => {
            this.toastService.success($localize`Replication request approved`);
            this.loadRequests();
          },
          error: () => {
            this.toastService.error($localize`Failed to approve request`);
          },
        });
      },
    });
  }

  denyRequest(request: ReplicationRequest): void {
    this.confirmationService.confirm({
      header: $localize`Deny Replication`,
      message: $localize`Deny replication to target ${request.targetIp}?`,
      acceptLabel: $localize`Deny`,
      rejectLabel: $localize`Cancel`,
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.service.deny(request.id).subscribe({
          next: () => {
            this.toastService.info($localize`Replication request denied`);
            this.loadRequests();
          },
          error: () => {
            this.toastService.error($localize`Failed to deny request`);
          },
        });
      },
    });
  }

  severityTag(severity: string): 'success' | 'warning' | 'danger' | 'info' {
    switch (severity) {
      case 'CRITICAL':
        return 'danger';
      case 'HIGH':
        return 'warning';
      case 'MEDIUM':
        return 'info';
      default:
        return 'success';
    }
  }

  statusTag(
    status: string
  ): 'success' | 'warning' | 'danger' | 'info' | 'secondary' {
    switch (status) {
      case 'APPROVED':
        return 'success';
      case 'PENDING':
        return 'warning';
      case 'DENIED':
        return 'danger';
      case 'EXPIRED':
        return 'secondary';
      default:
        return 'info';
    }
  }
}
