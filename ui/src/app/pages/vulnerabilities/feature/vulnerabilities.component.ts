import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { VulnerabilityListItem } from '../data-access/vulnerabilities.model';
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
    TooltipModule,
    InputTextModule,
    DropdownModule,
  ],
  templateUrl: './vulnerabilities.component.html',
  styleUrls: ['./vulnerabilities.component.scss'],
})
export class VulnerabilitiesComponent {
  private vulnService = inject(VulnerabilitiesService);
  private router = inject(Router);

  records = signal<VulnerabilityListItem[]>([]);
  totalSig = signal(0);
  loadingSig = signal(false);
  pageSig = signal(0);
  sizeSig = signal(10);

  searchQuery = '';
  selectedSeverity = '';

  severityOptions = [
    { label: $localize`All`, value: '' },
    { label: $localize`Low`, value: 'LOW' },
    { label: $localize`Medium`, value: 'MEDIUM' },
    { label: $localize`High`, value: 'HIGH' },
    { label: $localize`Critical`, value: 'CRITICAL' },
  ];

  onLazyLoad(event: TableLazyLoadEvent) {
    const first = event.first ?? 0;
    const rows = event.rows ?? this.sizeSig();
    const page = first / rows;
    this.pageSig.set(page);
    this.sizeSig.set(rows);
    this.loadRecords();
  }

  onSearch() {
    this.pageSig.set(0);
    this.loadRecords();
  }

  onSeverityChange() {
    this.pageSig.set(0);
    this.loadRecords();
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
        this.pageSig(),
        this.sizeSig(),
        this.searchQuery || undefined,
        this.selectedSeverity || undefined
      )
      .subscribe({
        next: res => {
          this.records.set(res.content);
          this.totalSig.set(res.totalElements);
          this.loadingSig.set(false);
        },
        error: () => this.loadingSig.set(false),
      });
  }
}
