import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { ToastService } from 'src/app/shared/services/toast.service';
import {
  RemediationAction,
  RemediationStrategy,
  RemediationType,
} from '../../data-access/remediations.model';
import { RemediationsService } from '../../data-access/remediations.service';

@Component({
  selector: 'app-strategies-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    DropdownModule,
    InputTextModule,
  ],
  templateUrl: './strategies-list.component.html',
  styleUrls: ['./strategies-list.component.scss'],
})
export class StrategiesListComponent {
  private destroyRef = inject(DestroyRef);
  private remediationsService = inject(RemediationsService);
  private toastService = inject(ToastService);

  strategies = signal<RemediationStrategy[]>([]);
  totalSig = signal(0);
  loadingSig = signal(false);
  pageSig = signal(0);
  sizeSig = signal(10);

  filterCveId = '';
  filterOperatingSystem = '';
  filterPackageName = '';
  filterRemediationType = '';
  filterAction = '';

  readonly emptyMessage = $localize`No remediation strategies found.`;

  osOptions = [
    { label: $localize`All`, value: '' },
    { label: 'ubuntu-20.04', value: 'ubuntu-20.04' },
    { label: 'ubuntu-22.04', value: 'ubuntu-22.04' },
    { label: 'debian-11', value: 'debian-11' },
    { label: 'debian-12', value: 'debian-12' },
  ];

  typeOptions = [
    { label: $localize`All`, value: '' },
    { label: $localize`Service Update`, value: 'SERVICE_UPDATE' },
    { label: $localize`Reboot Required`, value: 'REBOOT_REQUIRED' },
    { label: $localize`Kernel Update`, value: 'KERNEL_UPDATE' },
  ];

  actionOptions = [
    { label: $localize`All`, value: '' },
    { label: 'APT Upgrade', value: 'APT_UPGRADE' },
    { label: 'APT Install', value: 'APT_INSTALL' },
    { label: 'Config Update', value: 'CONFIG_UPDATE' },
    { label: 'Systemctl Restart', value: 'SYSTEMCTL_RESTART' },
    { label: 'Manual', value: 'MANUAL' },
  ];

  constructor() {
    this.loadStrategies();
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const first = event.first ?? 0;
    const rows = event.rows ?? this.sizeSig();
    const page = first / rows;
    this.pageSig.set(page);
    this.sizeSig.set(rows);
    this.loadStrategies();
  }

  onFilterChange(): void {
    this.pageSig.set(0);
    this.loadStrategies();
  }

  clearFilters(): void {
    this.filterCveId = '';
    this.filterOperatingSystem = '';
    this.filterPackageName = '';
    this.filterRemediationType = '';
    this.filterAction = '';
    this.pageSig.set(0);
    this.loadStrategies();
  }

  getTypeSeverity(
    type: RemediationType
  ): 'success' | 'danger' | 'warning' | 'info' | undefined {
    switch (type) {
      case 'SERVICE_UPDATE':
        return 'success';
      case 'REBOOT_REQUIRED':
        return 'warning';
      case 'KERNEL_UPDATE':
        return 'danger';
      default:
        return 'info';
    }
  }

  getTypeLabel(type: RemediationType): string {
    switch (type) {
      case 'SERVICE_UPDATE':
        return $localize`Service Update`;
      case 'REBOOT_REQUIRED':
        return $localize`Reboot Required`;
      case 'KERNEL_UPDATE':
        return $localize`Kernel Update`;
      default:
        return $localize`Unknown`;
    }
  }

  getActionLabel(action: RemediationAction): string {
    switch (action) {
      case 'APT_UPGRADE':
        return 'APT Upgrade';
      case 'APT_INSTALL':
        return 'APT Install';
      case 'CONFIG_UPDATE':
        return 'Config Update';
      case 'SYSTEMCTL_RESTART':
        return 'Systemctl Restart';
      case 'MANUAL':
        return 'Manual';
      default:
        return action;
    }
  }

  trackByStrategyId(_index: number, strategy: RemediationStrategy): string {
    return strategy.id;
  }

  private loadStrategies(): void {
    this.loadingSig.set(true);
    this.remediationsService
      .listStrategies(
        this.pageSig(),
        this.sizeSig(),
        this.filterCveId || undefined,
        this.filterOperatingSystem || undefined,
        this.filterPackageName || undefined,
        this.filterRemediationType || undefined,
        this.filterAction || undefined
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.strategies.set([...res.content]);
          this.totalSig.set(res.totalElements);
          this.loadingSig.set(false);
        },
        error: () => {
          this.loadingSig.set(false);
          this.toastService.error(
            $localize`Failed to load remediation strategies`
          );
        },
      });
  }
}
