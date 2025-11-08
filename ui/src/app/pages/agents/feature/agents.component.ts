import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabViewModule } from 'primeng/tabview';
import { AgentsListComponent } from './agents-list/agents-list.component';
import { AgentsMetricsComponent } from './agents-metrics/agents-metrics.component';

@Component({
  selector: 'app-agents',
  standalone: true,
  imports: [
    CommonModule,
    TabViewModule,
    AgentsListComponent,
    AgentsMetricsComponent,
  ],
  templateUrl: './agents.component.html',
  styleUrls: ['./agents.component.scss'],
})
export class AgentsComponent {}
