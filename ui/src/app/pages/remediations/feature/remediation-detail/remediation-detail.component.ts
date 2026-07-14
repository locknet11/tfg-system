import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DividerModule } from 'primeng/divider';
import { TagModule } from 'primeng/tag';
import { FieldsetModule } from 'primeng/fieldset';

import { RemediationRecord } from '../../data-access/remediations.model';
import { RemediationsService } from '../../data-access/remediations.service';

@Component({
  selector: 'app-remediation-detail',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    CardModule,
    DividerModule,
    TagModule,
    FieldsetModule,
  ],
  templateUrl: './remediation-detail.component.html',
  styleUrl: './remediation-detail.component.scss',
})
export class RemediationDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private remediationsService = inject(RemediationsService);

  record = signal<RemediationRecord | null>(null);
  loadingSig = signal(true);

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadRecord(id);
    }
  }

  goBack() {
    this.router.navigate(['/remediations']);
  }

  getStatusSeverity(
    status: string
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

  getRemediationTypeLabel(type: string): string {
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

  getStatusLabel(status: string): string {
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

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString();
  }

  private loadRecord(id: string) {
    this.loadingSig.set(true);
    this.remediationsService.get(id).subscribe({
      next: record => {
        this.record.set(record);
        this.loadingSig.set(false);
      },
      error: () => {
        this.loadingSig.set(false);
        this.router.navigate(['/remediations']);
      },
    });
  }
}
