import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputSwitchModule } from 'primeng/inputswitch';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { CheckboxModule } from 'primeng/checkbox';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import {
  AgentPlanInfo,
  AssignPlanRequest,
  Plan,
  StepAction,
  StepExecutionStatus,
} from '../../data-access/agents.model';
import { AgentsService } from '../../data-access/agents.service';
import { TemplatesService } from '../../../templates/data-access/templates.service';

@Component({
  selector: 'app-assign-plan-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    DialogModule,
    InputSwitchModule,
    DropdownModule,
    InputTextareaModule,
    CheckboxModule,
    ButtonModule,
    TooltipModule,
  ],
  templateUrl: './assign-plan-modal.component.html',
  styleUrls: ['./assign-plan-modal.component.scss'],
})
export class AssignPlanModalComponent implements OnInit, OnChanges {
  @Input() agentId!: string;
  @Input() showModal = false;
  @Output() modalClosed = new EventEmitter<void>();
  @Output() planAssigned = new EventEmitter<void>();

  useTemplate = false;
  selectedTemplateId: string | null = null;
  templates: { label: string; value: string }[] = [];
  planForm: FormGroup;
  submitting = false;

  currentPlan: AgentPlanInfo | null = null;
  loadingPlan = false;
  planLoadError = false;

  stepActionOptions = Object.values(StepAction).map(action => ({
    label: this.formatActionLabel(action),
    value: action,
  }));

  constructor(
    private fb: FormBuilder,
    private agentsService: AgentsService,
    private templatesService: TemplatesService,
    private messageService: MessageService
  ) {
    this.planForm = this.fb.group({
      notes: [''],
      allowTemplating: [false],
      steps: this.fb.array([], Validators.required),
    });
  }

  ngOnInit() {
    this.loadTemplates();
  }

  ngOnChanges(changes: SimpleChanges) {
    const justOpened =
      changes['showModal'] && changes['showModal'].currentValue === true;
    const agentSwitchedWhileOpen =
      changes['agentId'] && !changes['agentId'].firstChange && this.showModal;

    if (justOpened || agentSwitchedWhileOpen) {
      this.resetModalState();
      if (this.agentId) {
        this.loadCurrentPlan(this.agentId);
      }
    }
  }

  loadCurrentPlan(agentId: string) {
    this.loadingPlan = true;
    this.planLoadError = false;
    this.agentsService.getPlan(agentId).subscribe({
      next: plan => {
        // Ignore a stale response for an agent the dialog is no longer showing.
        if (agentId !== this.agentId || !this.showModal) {
          return;
        }
        this.currentPlan = plan;
        this.loadingPlan = false;
      },
      error: () => {
        if (agentId !== this.agentId || !this.showModal) {
          return;
        }
        this.planLoadError = true;
        this.loadingPlan = false;
      },
    });
  }

  loadTemplates() {
    this.templatesService.list('', 0, 100).subscribe({
      next: response => {
        this.templates = response.content.map(template => ({
          label: template.name,
          value: template.id,
        }));
      },
      error: err => {
        this.messageService.add({
          severity: 'error',
          summary: $localize`Error`,
          detail: $localize`Failed to load templates`,
        });
      },
    });
  }

  get canSubmit(): boolean {
    return this.useTemplate ? !!this.selectedTemplateId : this.planForm.valid;
  }

  toggleMode() {
    this.selectedTemplateId = null;
    this.resetPlanForm();
  }

  get stepsFormArray(): FormArray {
    return this.planForm.get('steps') as FormArray;
  }

  addStep() {
    const stepGroup = this.fb.group({
      action: [null, Validators.required],
    });
    this.stepsFormArray.push(stepGroup);
  }

  removeStep(index: number) {
    this.stepsFormArray.removeAt(index);
  }

  clearSteps() {
    while (this.stepsFormArray.length !== 0) {
      this.stepsFormArray.removeAt(0);
    }
  }

  submitForm() {
    if (this.submitting || !this.canSubmit) return;

    const request: AssignPlanRequest = {
      useTemplate: this.useTemplate,
      templateId: this.useTemplate
        ? this.selectedTemplateId || undefined
        : undefined,
      plan: !this.useTemplate ? this.buildPlanFromForm() : undefined,
    };

    this.submitting = true;
    this.agentsService
      .assignPlan(this.agentId, request)
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: $localize`Success`,
            detail: $localize`Plan assigned successfully`,
          });
          this.closeModal();
          this.planAssigned.emit();
        },
        error: err => {
          this.messageService.add({
            severity: 'error',
            summary: $localize`Error`,
            detail: $localize`Failed to assign plan`,
          });
        },
      })
      .add(() => {
        this.submitting = false;
      });
  }

  private buildPlanFromForm(): Plan {
    const formValue = this.planForm.value;
    return {
      notes: formValue.notes,
      allowTemplating: formValue.allowTemplating,
      steps: formValue.steps.map((step: any) => ({
        status: StepExecutionStatus.PENDING,
        action: step.action,
        logs: [],
      })),
    };
  }

  closeModal() {
    this.showModal = false;
    this.resetModalState();
    this.modalClosed.emit();
  }

  private resetModalState() {
    this.useTemplate = false;
    this.selectedTemplateId = null;
    this.submitting = false;
    this.resetPlanForm();
    this.currentPlan = null;
    this.loadingPlan = false;
    this.planLoadError = false;
  }

  private resetPlanForm() {
    this.clearSteps();
    this.planForm.reset({ notes: '', allowTemplating: false });
  }

  formatActionLabel(action: StepAction): string {
    switch (action) {
      case StepAction.SYSTEM_SCAN:
        return $localize`System scan`;
      case StepAction.SERVICE_SCAN:
        return $localize`Service scan`;
      case StepAction.NETWORK_SCAN:
        return $localize`Network scan`;
      case StepAction.GENERATE_REPORT:
        return $localize`Generate report`;
      case StepAction.SEND_REPORT:
        return $localize`Send report`;
      case StepAction.EXPLOITATION_KNOWLEDGE:
        return $localize`Exploitation knowledge`;
      case StepAction.REQUEST_REPLICATION:
        return $localize`Request replication`;
      case StepAction.EXECUTE_EXPLOIT:
        return $localize`Execute exploit`;
      case StepAction.TRANSFER_AGENT:
        return $localize`Transfer agent`;
      case StepAction.REPLICATE:
        return $localize`Replicate`;
      case StepAction.SELF_DESTRUCT:
        return $localize`Self destruct`;
      default:
        return action;
    }
  }
}
