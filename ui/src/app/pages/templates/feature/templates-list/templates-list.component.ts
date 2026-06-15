import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputTextModule } from 'primeng/inputtext';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';

import { PlanTemplate } from '../../data-access/templates.model';
import { TemplatesService } from '../../data-access/templates.service';
import { CreateTemplateModalComponent } from '../modals/create-template-modal/create-template-modal.component';
import { EditTemplateModalComponent } from '../modals/edit-template-modal/edit-template-modal.component';

@Component({
  selector: 'app-templates-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    ConfirmDialogModule,
    ToastModule,
    CreateTemplateModalComponent,
    EditTemplateModalComponent,
  ],
  templateUrl: './templates-list.component.html',
  styleUrls: ['./templates-list.component.scss'],
  providers: [ConfirmationService, MessageService],
})
export class TemplatesListComponent {
  private destroyRef = inject(DestroyRef);
  private templatesService = inject(TemplatesService);
  private confirm = inject(ConfirmationService);
  private messages = inject(MessageService);

  private searchSubject = new Subject<string>();

  createTemplateModal = viewChild.required(CreateTemplateModalComponent);
  editTemplateModal = viewChild.required(EditTemplateModalComponent);

  templatesSig = signal<PlanTemplate[]>([]);
  totalSig = signal(0);
  loadingSig = signal(false);
  querySig = signal('');
  pageSig = signal(0);
  sizeSig = signal(10);

  readonly emptyMessage = $localize`No templates found`;
  readonly emptySearchMessage = $localize`No templates match your search`;

  filteredTemplates = computed(() => this.templatesSig());

  constructor() {
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(query => {
        this.querySig.set(query);
        this.pageSig.set(0);
        this.loadTemplates();
      });
  }

  ngOnInit() {
    this.loadTemplates();
  }

  onSearchInput(value: string) {
    this.searchSubject.next(value.trim());
  }

  onLazyLoad(event: TableLazyLoadEvent) {
    const newPage = Math.floor(
      (event.first || 0) / (event.rows || this.sizeSig())
    );
    const newSize = event.rows || this.sizeSig();
    this.pageSig.set(newPage);
    this.sizeSig.set(newSize);
    this.loadTemplates();
  }

  editTemplate(template: PlanTemplate) {
    this.editTemplateModal().show(template);
  }

  createTemplate() {
    this.createTemplateModal().show();
  }

  deleteTemplate(template: PlanTemplate) {
    this.confirm.confirm({
      header: $localize`Delete Template`,
      message: $localize`Are you sure you want to delete ${template.name}?`,
      acceptLabel: $localize`Delete`,
      rejectLabel: $localize`Cancel`,
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.templatesService.delete(template.id).subscribe({
          next: () => {
            this.messages.add({
              severity: 'success',
              summary: $localize`Success`,
              detail: $localize`Template deleted`,
            });
            this.loadTemplates();
          },
          error: () =>
            this.messages.add({
              severity: 'error',
              summary: $localize`Error`,
              detail: $localize`Failed to delete template`,
            }),
        });
      },
    });
  }

  loadTemplates() {
    this.loadingSig.set(true);
    this.templatesService
      .list(this.querySig(), this.pageSig(), this.sizeSig())
      .subscribe({
        next: res => {
          this.templatesSig.set(res.content);
          this.totalSig.set(res.totalElements);
          this.loadingSig.set(false);
        },
        error: () => this.loadingSig.set(false),
      });
  }
}
