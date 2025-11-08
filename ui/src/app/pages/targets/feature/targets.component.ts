import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { ToastService } from 'src/app/shared/services/toast.service';
import { Target } from '../data-access/targets.model';

@Component({
  selector: 'app-targets',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    TagModule,
    ConfirmDialogModule,
  ],
  providers: [ConfirmationService],
  templateUrl: './targets.component.html',
  styleUrls: ['./targets.component.scss'],
})
export class TargetsComponent implements OnInit {
  targets: Target[] = [
    {
      id: '1',
      systemName: 'Servidor Web Principal',
      ipOrDomain: '10.0.0.1',
      status: 'online',
      assignedAgent: 'Agente-001',
    },
    {
      id: '2',
      systemName: 'Base de Datos Clientes',
      ipOrDomain: 'db.clientes.prod',
      status: 'offline',
      assignedAgent: 'Agente-002',
    },
    {
      id: '3',
      systemName: 'API Gateway',
      ipOrDomain: 'api.cloudguard.com',
      status: 'in_review',
      assignedAgent: 'Agente-003',
    },
    {
      id: '4',
      systemName: 'Servidor de Staging',
      ipOrDomain: '172.16.0.10',
      status: 'online',
      assignedAgent: 'Agente-004',
    },
  ];

  constructor(
    private confirmationService: ConfirmationService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {}

  addTarget() {
    this.toastService.success($localize`Add target functionality`);
  }

  editTarget(target: Target) {
    this.toastService.success($localize`Edit target: ${target.systemName}`);
  }

  deleteConfirm(event: Event, target: Target) {
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

  private deleteTarget(target: Target) {
    this.targets = this.targets.filter(t => t.id !== target.id);
    this.toastService.success($localize`Target deleted successfully`);
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'online':
        return $localize`Online`;
      case 'offline':
        return $localize`Offline`;
      case 'in_review':
        return $localize`In Review`;
      default:
        return status;
    }
  }

  getStatusSeverity(
    status: string
  ): 'success' | 'danger' | 'warning' | 'info' | 'secondary' | 'contrast' {
    switch (status) {
      case 'online':
        return 'success';
      case 'offline':
        return 'danger';
      case 'in_review':
        return 'warning';
      default:
        return 'info';
    }
  }
}
