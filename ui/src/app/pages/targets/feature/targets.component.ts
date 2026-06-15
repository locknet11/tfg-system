import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputTextModule } from 'primeng/inputtext';
import { ConfirmationService } from 'primeng/api';
import { ToastService } from 'src/app/shared/services/toast.service';
import { TargetsService } from '../data-access/targets.service';
import { TargetInfo } from '../data-access/targets.model';
import { CreateTargetModalComponent } from './modals/create-target-modal/create-target-modal.component';
import { EditTargetModalComponent } from './modals/edit-target-modal/edit-target-modal.component';
import { AgentSetupModalComponent } from './modals/agent-setup-modal/agent-setup-modal.component';

@Component({
  selector: 'app-targets',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    ConfirmDialogModule,
    InputTextModule,
    CreateTargetModalComponent,
    EditTargetModalComponent,
    AgentSetupModalComponent,
  ],
  providers: [ConfirmationService, TargetsService],
  templateUrl: './targets.component.html',
  styleUrls: ['./targets.component.scss'],
})
export class TargetsComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  private confirmationService = inject(ConfirmationService);
  private toastService = inject(ToastService);
  private targetsService = inject(TargetsService);

  private searchSubject = new Subject<string>();

  targets = signal<TargetInfo[]>([]);
  totalRecords = signal(0);
  rows = signal(10);
  loading = signal(false);
  query = signal('');

  readonly emptyMessage = $localize`No targets found`;
  readonly emptySearchMessage = $localize`No targets match your search`;

  createTargetModal = viewChild.required(CreateTargetModalComponent);
  editTargetModal = viewChild.required(EditTargetModalComponent);
  agentSetupModal = viewChild.required(AgentSetupModalComponent);

  constructor() {
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(query => {
        this.query.set(query);
        this.loadTargets();
      });
  }

  ngOnInit(): void {}

  loadTargets(pageEvent?: TableLazyLoadEvent) {
    let page = 0;
    let size = 10;
    if (pageEvent && pageEvent.first != null && pageEvent.rows) {
      page = pageEvent.first / pageEvent.rows;
      size = pageEvent.rows;
    }
    this.loading.set(true);
    this.targetsService.list(this.query(), page, size).subscribe({
      next: res => {
        this.targets.set(res.content);
        this.totalRecords.set(res.totalElements);
        this.rows.set(res.size);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toastService.error($localize`Failed to load targets`);
      },
    });
  }

  onSearchInput(value: string) {
    this.searchSubject.next(value.trim());
  }

  addTarget() {
    this.createTargetModal().show();
  }

  editTarget(target: TargetInfo) {
    this.editTargetModal().show(target);
  }

  showAgentSetup(target: TargetInfo) {
    const selectedOrgStr = localStorage.getItem('selectedOrganization');
    const selectedProjectStr = localStorage.getItem('selectedProject');

    if (!selectedOrgStr || !selectedProjectStr) {
      this.toastService.error(
        $localize`Organization or project information not found`
      );
      return;
    }

    if (!target.preauthCode) {
      this.toastService.error(
        $localize`Pre-authorization code missing for this target`
      );
      return;
    }

    const selectedOrg = JSON.parse(selectedOrgStr);
    const selectedProject = JSON.parse(selectedProjectStr);

    this.agentSetupModal().show(
      selectedOrg.organizationIdentifier,
      selectedProject.projectIdentifier,
      target.uniqueId,
      target.preauthCode
    );
  }

  deleteConfirm(event: Event, target: TargetInfo) {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: $localize`Do you want to delete this target?`,
      header: $localize`Delete target`,
      icon: 'pi pi-info-circle',
      acceptButtonStyleClass: 'p-button-danger p-button-text',
      rejectButtonStyleClass: 'p-button-text p-button-text',
      acceptIcon: 'none',
      rejectIcon: 'none',
      accept: () => {
        this.deleteTarget(target);
      },
      reject: () => {},
    });
  }

  private deleteTarget(target: TargetInfo) {
    this.targetsService.deleteTarget(target.id).subscribe({
      next: () => {
        this.toastService.success($localize`Target deleted successfully`);
        this.loadTargets();
      },
      error: () => {
        this.toastService.error($localize`Failed to delete target`);
      },
    });
  }

  onTargetCreated() {
    this.loadTargets();
  }

  onTargetUpdated() {
    this.loadTargets();
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ONLINE':
        return $localize`Online`;
      case 'OFFLINE':
        return $localize`Offline`;
      case 'IN_REVIEW':
        return $localize`In Review`;
      default:
        return status;
    }
  }

  getStatusSeverity(
    status: string
  ): 'success' | 'danger' | 'warning' | 'info' | 'secondary' | 'contrast' {
    switch (status) {
      case 'ONLINE':
        return 'success';
      case 'OFFLINE':
        return 'danger';
      case 'IN_REVIEW':
        return 'warning';
      default:
        return 'info';
    }
  }
}
