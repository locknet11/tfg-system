import { Component, OnInit, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
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
    CreateTargetModalComponent,
    EditTargetModalComponent,
    AgentSetupModalComponent,
  ],
  providers: [ConfirmationService, TargetsService],
  templateUrl: './targets.component.html',
  styleUrls: ['./targets.component.scss'],
})
export class TargetsComponent implements OnInit {
  targets = signal<TargetInfo[]>([]);
  totalRecords = signal(0);
  rows = signal(10);

  createTargetModal = viewChild.required(CreateTargetModalComponent);
  editTargetModal = viewChild.required(EditTargetModalComponent);
  agentSetupModal = viewChild.required(AgentSetupModalComponent);

  constructor(
    private confirmationService: ConfirmationService,
    private toastService: ToastService,
    private targetsService: TargetsService
  ) {}

  ngOnInit(): void {
    // this.getTargets();
  }

  getTargets(pageEvent?: TableLazyLoadEvent) {
    let page = 0;
    let size = 10;
    if (pageEvent && pageEvent.first && pageEvent.rows) {
      page = pageEvent.first / pageEvent.rows;
      size = pageEvent.rows;
    }
    this.targetsService.getTargets(page, size).subscribe({
      next: res => {
        this.targets.set(res.content);
        this.totalRecords.set(res.totalElements);
        this.rows.set(res.size);
      },
    });
  }

  addTarget() {
    this.createTargetModal().show();
  }

  editTarget(target: TargetInfo) {
    this.editTargetModal().show(target);
  }

  showAgentSetup(target: TargetInfo) {
    // Get organization and project from localStorage
    const selectedOrgStr = localStorage.getItem('selectedOrganization');
    const selectedProjectStr = localStorage.getItem('selectedProject');
    
    if (!selectedOrgStr || !selectedProjectStr) {
      this.toastService.error($localize`Organization or project information not found`);
      return;
    }

    const selectedOrg = JSON.parse(selectedOrgStr);
    const selectedProject = JSON.parse(selectedProjectStr);

    // Use short identifiers instead of MongoDB IDs
    this.agentSetupModal().show(selectedOrg.organizationIdentifier, selectedProject.projectIdentifier, target.uniqueId);
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
      next: res => {
        this.toastService.success($localize`Target deleted successfully`);
        this.getTargets();
      },
      error: err => {
        console.error(err);
      },
    });
  }

  onTargetCreated(target?: any) {
    this.getTargets();
  }

  onTargetUpdated() {
    this.getTargets();
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
