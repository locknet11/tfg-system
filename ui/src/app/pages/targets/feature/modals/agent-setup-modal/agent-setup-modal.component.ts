import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-agent-setup-modal',
  standalone: true,
  imports: [CommonModule, DialogModule, ButtonModule, InputTextModule],
  templateUrl: './agent-setup-modal.component.html',
  styleUrls: ['./agent-setup-modal.component.scss'],
})
export class AgentSetupModalComponent {
  visible = signal(false);
  agentUrl = signal('');
  curlCommand = signal('');

  show(organizationId: string, projectId: string, uniqueId: string) {
    const baseUrl = environment.baseUrl;
    const url = `${baseUrl}/api/agent/${organizationId}/${projectId}/${uniqueId}`;
    this.agentUrl.set(url);
    this.curlCommand.set(`curl -sSL ${url} | bash`);
    this.visible.set(true);
  }

  hide() {
    this.visible.set(false);
  }

  copyToClipboard(text: string) {
    navigator.clipboard.writeText(text);
  }
}
