import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { VulnerabilityListItem, VulnerabilityStatistics } from '../data-access/vulnerabilities.model';
import { VulnerabilitiesService } from '../data-access/vulnerabilities.service';

@Component({
  selector: 'app-vulnerabilities',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    TagModule,
    SkeletonModule,
    TooltipModule,
    InputTextModule,
    DropdownModule,
  ],
  templateUrl: './vulnerabilities.component.html',
  styleUrls: ['./vulnerabilities.component.scss'],
})
export class VulnerabilitiesComponent {
  private destroyRef = inject(DestroyRef);
  private vulnService = inject(VulnerabilitiesService);
  private router = inject(Router);

  private searchSubject = new Subject<string>();

  records = signal<VulnerabilityListItem[]>([]);
  statistics = signal<VulnerabilityStatistics | null>(null);
  statsLoading = signal(false);
  totalSig = signal(0);
  loadingSig = signal(false);
  pageSig = signal(0);
  sizeSig = signal(10);
  querySig = signal('');

  readonly emptyMessage = $localize`No vulnerability records found.`;
  readonly emptySearchMessage = $localize`No vulnerabilities match your search`;

  selectedSeverity = '';

  severityOptions = [
    { label: $localize`All`, value: '' },
    { label: $localize`Low`, value: 'LOW' },
    { label: $localize`Medium`, value: 'MEDIUM' },
    { label: $localize`High`, value: 'HIGH' },
    { label: $localize`Critical`, value: 'CRITICAL' },
  ];

  constructor() {
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(query => {
        this.querySig.set(query);
        this.pageSig.set(0);
        this.loadRecords();
        this.loadStatistics();
      });
  }

  onLazyLoad(event: TableLazyLoadEvent) {
    const first = event.first ?? 0;
    const rows = event.rows ?? this.sizeSig();
    const page = first / rows;
    this.pageSig.set(page);
    this.sizeSig.set(rows);
    this.loadRecords();
  }

  onSearchInput(value: string) {
    this.searchSubject.next(value.trim());
  }

  onSeverityChange() {
    this.pageSig.set(0);
    this.loadRecords();
    this.loadStatistics();
  }

  viewDetail(record: VulnerabilityListItem) {
    this.router.navigate(['/vulnerabilities', record.serviceKey]);
  }

  getSeverityClass(
    severity: string | null
  ): 'danger' | 'warning' | 'info' | 'success' | undefined {
    switch (severity) {
      case 'CRITICAL':
        return 'danger';
      case 'HIGH':
        return 'warning';
      case 'MEDIUM':
        return 'info';
      case 'LOW':
        return 'success';
      default:
        return undefined;
    }
  }

  private loadRecords() {
    this.loadingSig.set(true);
    this.vulnService
      .list(
        this.querySig(),
        this.pageSig(),
        this.sizeSig(),
        this.selectedSeverity || undefined
      )
      .subscribe({
        next: (res) => {
          this.records.set(res.content);
          this.totalSig.set(res.totalElements);
          this.loadingSig.set(false);
        },
        error: () => this.loadingSig.set(false),
      });
  }

  private loadStatistics() {
    this.statsLoading.set(true);
    this.vulnService
      .getStatistics(
        this.querySig() || undefined,
        this.selectedSeverity || undefined
      )
      .subscribe({
        next: (stats) => {
          this.statistics.set(stats);
          this.statsLoading.set(false);
        },
        error: () => this.statsLoading.set(false),
      });
  }
}
