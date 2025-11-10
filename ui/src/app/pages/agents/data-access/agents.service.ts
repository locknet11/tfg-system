import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { Agent, AgentStatus, AgentsList } from './agents.model';

@Injectable()
export class AgentsService {
  private mockAgents: Agent[] = this.generateMockAgents();

  list(page: number, size: number, sort?: string, order: 'asc' | 'desc' = 'desc', filter: string = ''): Observable<AgentsList> {
    let data = [...this.mockAgents];

    if (filter) {
      const q = filter.toLowerCase();
      data = data.filter(a => a.name.toLowerCase().includes(q) || a.targetSystem?.toLowerCase().includes(q));
    }

    if (sort) {
      data.sort((a: any, b: any) => {
        const dir = order === 'asc' ? 1 : -1;
        const av = a[sort];
        const bv = b[sort];
        if (av instanceof Date && bv instanceof Date) {
          return (av.getTime() - bv.getTime()) * dir;
        }
        return (av > bv ? 1 : av < bv ? -1 : 0) * dir;
      });
    }

    const totalElements = data.length;
    const start = page * size;
    const end = start + size;
    const content = data.slice(start, end);

    const totalPages = Math.ceil(totalElements / size) || 1;

    const resp: AgentsList = { content, page, size, totalPages, totalElements };
    return of(resp).pipe(delay(300));
  }

  delete(id: string): Observable<void> {
    this.mockAgents = this.mockAgents.filter(a => a.id !== id);
    return of(void 0).pipe(delay(200));
  }

  private generateMockAgents(): Agent[] {
    const statuses = [
      AgentStatus.IN_CREATION,
      AgentStatus.CREATED,
      AgentStatus.ACTIVE,
      AgentStatus.UNRESPONSIVE,
      AgentStatus.KILLED,
    ];

    const list: Agent[] = [];
    for (let i = 1; i <= 48; i++) {
      const status = statuses[i % statuses.length];
      list.push({
        id: `AGT-${i.toString().padStart(3, '0')}`,
        name: `Agent ${i}`,
        status,
        version: `v${(1 + (i % 4))}.0.${i % 10}`,
        lastConnection: new Date(Date.now() - (i * 3600 * 1000)),
        targetSystem: `Target-${(i % 7) + 1}`,
      });
    }
    return list;
  }
}