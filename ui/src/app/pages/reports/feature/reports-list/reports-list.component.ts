import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { MultiSelectModule } from 'primeng/multiselect';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { ToastService } from 'src/app/shared/services/toast.service';
import { TargetInfo } from '../../../targets/data-access/targets.model';
import { TargetsService } from '../../../targets/data-access/targets.service';
import { RemediationStatus } from '../../../remediations/data-access/remediations.model';
import {
  GenerationType,
  REPORT_EMPTY_RESULT,
  ReportGenerateRequest,
  ReportInfo,
  ReportSeverity,
} from '../../data-access/reports.model';
import { ReportsService } from '../../data-access/reports.service';

interface Option<T> {
  readonly label: string;
  readonly value: T;
}

@Component({
  selector: 'app-reports-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    CardModule,
    DropdownModule,
    MultiSelectModule,
    CalendarModule,
  ],
  providers: [TargetsService],
  templateUrl: './reports-list.component.html',
  styleUrls: ['./reports-list.component.scss'],
})
export class ReportsListComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly reportsService = inject(ReportsService);
  private readonly targetsService = inject(TargetsService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  readonly reports = signal<ReportInfo[]>([]);
  readonly totalSig = signal(0);
  readonly loadingSig = signal(false);
  readonly generatingSig = signal(false);
  readonly pageSig = signal(0);
  readonly sizeSig = signal(20);
  readonly targetOptions = signal<Option<string | null>[]>([]);

  readonly emptyMessage = $localize`No reports generated yet.`;

  selectedTargetId: string | null = null;
  dateFrom: Date | null = null;
  dateTo: Date | null = null;
  selectedSeverities: ReportSeverity[] = [];
  selectedStatuses: RemediationStatus[] = [];

  readonly severityOptions: Option<ReportSeverity>[] = [
    { label: $localize`Critical`, value: 'CRITICAL' },
    { label: $localize`High`, value: 'HIGH' },
    { label: $localize`Medium`, value: 'MEDIUM' },
    { label: $localize`Low`, value: 'LOW' },
    { label: $localize`Unknown`, value: 'UNKNOWN' },
  ];

  readonly statusOptions: Option<RemediationStatus>[] = [
    { label: $localize`Success`, value: 'SUCCESS' },
    { label: $localize`Failed`, value: 'FAILED' },
    { label: $localize`Pending`, value: 'PENDING' },
    { label: $localize`In Progress`, value: 'IN_PROGRESS' },
    { label: $localize`Skipped`, value: 'SKIPPED' },
    { label: $localize`Pending Reboot`, value: 'PENDING_REBOOT' },
  ];

  ngOnInit(): void {
    this.loadTargets();
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const first = event.first ?? 0;
    const rows = event.rows ?? this.sizeSig();
    this.pageSig.set(first / rows);
    this.sizeSig.set(rows);
    this.loadReports();
  }

  generate(): void {
    this.generatingSig.set(true);
    const request: ReportGenerateRequest = {
      targetId: this.selectedTargetId ?? undefined,
      from: this.dateFrom ? this.dateFrom.toISOString() : undefined,
      to: this.dateTo ? this.dateTo.toISOString() : undefined,
      severities: this.selectedSeverities,
      statuses: this.selectedStatuses,
    };

    this.reportsService
      .generate(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: report => {
          this.generatingSig.set(false);
          this.router.navigate(['/reports', report.id]);
        },
        error: (err: HttpErrorResponse) => {
          this.generatingSig.set(false);
          if (this.isEmptyResult(err)) {
            this.toastService.info(
              $localize`No data matches the selected filters.`
            );
          } else {
            this.toastService.error($localize`Failed to generate report.`);
          }
        },
      });
  }

  viewDetail(report: ReportInfo): void {
    this.router.navigate(['/reports', report.id]);
  }

  getGenerationTypeLabel(type: GenerationType): string {
    return type === 'SCHEDULED' ? $localize`Automatic` : $localize`On demand`;
  }

  private isEmptyResult(err: HttpErrorResponse): boolean {
    return (
      err.status === 422 && err.error?.description?.code === REPORT_EMPTY_RESULT
    );
  }

  private loadReports(): void {
    this.loadingSig.set(true);
    this.reportsService
      .list(this.pageSig(), this.sizeSig())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.reports.set(res.content);
          this.totalSig.set(res.totalElements);
          this.loadingSig.set(false);
        },
        error: () => {
          this.loadingSig.set(false);
          this.toastService.error($localize`Failed to load reports.`);
        },
      });
  }

  private loadTargets(): void {
    this.targetsService
      .list('', 0, 100)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          const options: Option<string | null>[] = [
            { label: $localize`All targets`, value: null },
            ...res.content.map((t: TargetInfo) => ({
              label: t.systemName,
              value: t.id,
            })),
          ];
          this.targetOptions.set(options);
        },
        error: () => {
          this.targetOptions.set([
            { label: $localize`All targets`, value: null },
          ]);
        },
      });
  }
}
