import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { AgentsService } from '../../data-access/agents.service';
import { Agent, AgentStatus } from '../../data-access/agents.model';

@Component({
  selector: 'app-agents-list',
  standalone: true,
  imports: [CommonModule, TableModule, ButtonModule, TagModule, InputTextModule, TooltipModule, ConfirmDialogModule, ToastModule],
  templateUrl: './agents-list.component.html',
  styleUrls: ['./agents-list.component.scss'],
  providers: [AgentsService, ConfirmationService, MessageService],
})
export class AgentsListComponent {
  agents = signal<Agent[]>([]);
  total = signal<number>(0);
  loading = signal<boolean>(false);

  page = signal<number>(0);
  size = signal<number>(10);

  AgentStatus = AgentStatus;

  constructor(private agentsService: AgentsService, private confirm: ConfirmationService, private messages: MessageService) {}

  fetch(page: number, size: number) {
    this.loading.set(true);
    this.agentsService.list(page, size).subscribe({
      next: resp => {
        this.agents.set(resp.content);
        this.total.set(resp.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messages.add({ severity: 'error', summary: $localize`Error`, detail: $localize`Failed to load agents` });
      }
    });
  }

  onLazyLoad(event: any) {
    const newPage = Math.floor((event.first || 0) / (event.rows || this.size()));
    const newSize = event.rows || this.size();
    this.page.set(newPage);
    this.size.set(newSize);
    this.fetch(newPage, newSize);
  }

  statusSeverity(status: AgentStatus): 'success' | 'info' | 'warning' | 'danger' | 'secondary' {
    switch (status) {
      case AgentStatus.ACTIVE:
        return 'success';
      case AgentStatus.IN_CREATION:
        return 'info';
      case AgentStatus.CREATED:
        return 'secondary';
      case AgentStatus.UNRESPONSIVE:
        return 'warning';
      case AgentStatus.KILLED:
      default:
        return 'danger';
    }
  }

  statusLabel(status: AgentStatus): string {
    switch (status) {
      case AgentStatus.IN_CREATION:
        return $localize`In Creation`;
      case AgentStatus.CREATED:
        return $localize`Created`;
      case AgentStatus.ACTIVE:
        return $localize`Active`;
      case AgentStatus.UNRESPONSIVE:
        return $localize`Unresponsive`;
      case AgentStatus.KILLED:
        return $localize`Killed`;
      default:
        return status as unknown as string;
    }
  }

  delete(agent: Agent) {
    this.confirm.confirm({
      header: $localize`Delete Agent`,
      message: $localize`Are you sure you want to delete ${agent.name}?`,
      acceptLabel: $localize`Delete`,
      rejectLabel: $localize`Cancel`,
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.agentsService.delete(agent.id).subscribe({
          next: () => {
            this.messages.add({ severity: 'success', summary: $localize`Success`, detail: $localize`Agent deleted` });
            this.fetch(this.page(), this.size());
          },
          error: () => this.messages.add({ severity: 'error', summary: $localize`Error`, detail: $localize`Failed to delete agent` })
        });
      }
    });
  }
}
