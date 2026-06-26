import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { ToastService } from 'src/app/shared/services/toast.service';
import {
  RemediationRecord,
  RemediationStatus,
  RemediationType,
} from '../../data-access/remediations.model';
import { RemediationsService } from '../../data-access/remediations.service';

@Component({
  selector: 'app-remediations-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    DropdownModule,
  ],
  templateUrl: './remediations-list.component.html',
  styleUrls: ['./remediations-list.component.scss'],
})
export class RemediationsListComponent {
  private destroyRef = inject(DestroyRef);
  private remediationsService = inject(RemediationsService);
  private router = inject(Router);
  private toastService = inject(ToastService);

  records = signal<RemediationRecord[]>([]);
  totalSig = signal(0);
  loadingSig = signal(false);
  pageSig = signal(0);
  sizeSig = signal(10);

  readonly emptyMessage = $localize`No remediation records found.`;

  selectedStatus = '';

  statusOptions = [
    { label: $localize`All`, value: '' },
    { label: $localize`Success`, value: 'SUCCESS' },
    { label: $localize`Failed`, value: 'FAILED' },
    { label: $localize`Pending`, value: 'PENDING' },
    { label: $localize`In Progress`, value: 'IN_PROGRESS' },
    { label: $localize`Skipped`, value: 'SKIPPED' },
    { label: $localize`Pending Reboot`, value: 'PENDING_REBOOT' },
  ];

  constructor() {
    this.loadRecords();
  }

  onLazyLoad(event: TableLazyLoadEvent) {
    const first = event.first ?? 0;
    const rows = event.rows ?? this.sizeSig();
    const page = first / rows;
    this.pageSig.set(page);
    this.sizeSig.set(rows);
    this.loadRecords();
  }

  onStatusChange() {
    this.pageSig.set(0);
    this.loadRecords();
  }

  viewDetail(record: RemediationRecord) {
    this.router.navigate(['/remediations', record.id]);
  }

  getStatusSeverity(
    status: RemediationStatus
  ): 'success' | 'danger' | 'warning' | 'info' | undefined {
    switch (status) {
      case 'SUCCESS':
        return 'success';
      case 'FAILED':
        return 'danger';
      case 'PENDING':
      case 'IN_PROGRESS':
        return 'warning';
      case 'SKIPPED':
      case 'PENDING_REBOOT':
        return 'info';
      default:
        return undefined;
    }
  }

  getStatusLabel(status: RemediationStatus): string {
    switch (status) {
      case 'SUCCESS':
        return $localize`Success`;
      case 'FAILED':
        return $localize`Failed`;
      case 'PENDING':
        return $localize`Pending`;
      case 'IN_PROGRESS':
        return $localize`In Progress`;
      case 'SKIPPED':
        return $localize`Skipped`;
      case 'PENDING_REBOOT':
        return $localize`Pending Reboot`;
      default:
        return status;
    }
  }

  getRemediationTypeLabel(type: RemediationType): string {
    switch (type) {
      case 'SERVICE_UPDATE':
        return $localize`Service Update`;
      case 'REBOOT_REQUIRED':
        return $localize`Reboot Required`;
      case 'KERNEL_UPDATE':
        return $localize`Kernel Update`;
      case 'UNKNOWN':
      default:
        return $localize`Unknown`;
    }
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString();
  }

  private loadRecords() {
    this.loadingSig.set(true);
    this.remediationsService
      .list(
        this.pageSig(),
        this.sizeSig(),
        (this.selectedStatus as RemediationStatus) || undefined
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.records.set(res.content);
          this.totalSig.set(res.totalElements);
          this.loadingSig.set(false);
        },
        error: () => {
          this.loadingSig.set(false);
          this.toastService.error(
            $localize`Failed to load remediation records`
          );
        },
      });
  }
}
