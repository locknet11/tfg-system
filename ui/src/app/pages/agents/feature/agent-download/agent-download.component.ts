import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { AgentsService } from '../../data-access/agents.service';
import { AgentPlatformInfo } from '../../data-access/agents.model';

@Component({
  selector: 'app-agent-download',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DialogModule,
    DropdownModule,
    ButtonModule,
    ProgressSpinnerModule,
    MessageModule,
  ],
  templateUrl: './agent-download.component.html',
  styleUrls: ['./agent-download.component.scss'],
})
export class AgentDownloadComponent implements OnChanges {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() closed = new EventEmitter<void>();

  private readonly agentsService = inject(AgentsService);

  readonly platforms = signal<AgentPlatformInfo[]>([]);
  readonly selectedPlatform = signal<AgentPlatformInfo | undefined>(undefined);
  readonly loading = signal(false);
  readonly error = signal<string | undefined>(undefined);

  readonly dialogHeader = $localize`Download Agent`;
  readonly loadingPlatforms = $localize`Loading available platforms...`;
  readonly noPlatforms = $localize`No agent binaries available for download.`;
  readonly selectPlatform = $localize`Select a platform`;
  readonly downloadLabel = $localize`Download`;
  readonly cancelLabel = $localize`Cancel`;
  readonly versionLabel = $localize`Version`;
  readonly sizeLabel = $localize`File size`;
  readonly hashLabel = $localize`Blake3 hash`;
  readonly downloadError = $localize`Failed to load platforms`;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible) {
      this.loadPlatforms();
    }
  }

  loadPlatforms(): void {
    this.loading.set(true);
    this.error.set(undefined);
    this.agentsService.getDownloadPlatforms().subscribe({
      next: platforms => {
        this.platforms.set(platforms);
        this.loading.set(false);
        if (platforms.length === 1) {
          this.selectedPlatform.set(platforms[0]);
        }
      },
      error: () => {
        this.error.set(this.downloadError);
        this.loading.set(false);
      },
    });
  }

  get selectedPlatformDetails(): string {
    const p = this.selectedPlatform();
    if (!p) return '';
    const sizeMB = (p.fileSizeBytes / (1024 * 1024)).toFixed(1);
    return `${p.label} — ${this.versionLabel}: ${p.agentVersion} — ${this.sizeLabel}: ${sizeMB} MB`;
  }

  download(): void {
    const platform = this.selectedPlatform();
    if (!platform) return;
    this.agentsService.downloadAgent(platform.platform);
    this.close();
  }

  close(): void {
    this.visibleChange.emit(false);
    this.closed.emit();
    this.selectedPlatform.set(undefined);
    this.error.set(undefined);
  }
}
