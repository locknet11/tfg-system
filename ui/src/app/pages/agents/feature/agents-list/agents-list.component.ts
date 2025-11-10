import { Component, effect, signal } from '@angular/core';
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
  // signals for state
  agents = signal<Agent[]>([]);
  total = signal<number>(0);
  loading = signal<boolean>(false);

  page = signal<number>(0);
  size = signal<number>(10);
  sortField = signal<string>('lastConnection');
  sortOrder = signal<1 | -1>(-1);
  filter = signal<string>('');

  AgentStatus = AgentStatus; // expose enum to template

  constructor(private agentsService: AgentsService, private confirm: ConfirmationService, private messages: MessageService) {
    effect(() => {
      // trigger list whenever relevant params change
      const p = this.page();
      const s = this.size();
      const f = this.filter();
      const sf = this.sortField();
      const so = this.sortOrder();
      this.fetch(p, s, sf, so, f);
    }, { allowSignalWrites: true });
  }

  fetch(page: number, size: number, sortField: string, sortOrder: 1 | -1, filter: string) {
    this.loading.set(true);
    const order = sortOrder === 1 ? 'asc' : 'desc';
    this.agentsService.list(page, size, sortField, order, filter).subscribe(resp => {
      this.agents.set(resp.content);
      this.total.set(resp.totalElements);
      this.loading.set(false);
    });
  }

  onLazyLoad(event: any) {
    this.page.set(Math.floor((event.first || 0) / (event.rows || this.size())));
    this.size.set(event.rows || this.size());
    if (event.sortField) this.sortField.set(event.sortField);
    if (event.sortOrder) this.sortOrder.set(event.sortOrder);
  }

  onSearch(value: string) {
    this.filter.set(value || '');
    this.page.set(0);
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
            this.fetch(this.page(), this.size(), this.sortField(), this.sortOrder(), this.filter());
          },
          error: () => this.messages.add({ severity: 'error', summary: $localize`Error`, detail: $localize`Failed to delete agent` })
        });
      }
    });
  }
}
