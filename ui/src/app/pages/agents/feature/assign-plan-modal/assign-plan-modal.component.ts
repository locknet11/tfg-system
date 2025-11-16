import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputSwitchModule } from 'primeng/inputswitch';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { CheckboxModule } from 'primeng/checkbox';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { AssignPlanRequest, Plan, Step, StepAction, StepExecutionStatus } from '../../data-access/agents.model';
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
    TableModule,
    ButtonModule
  ],
  templateUrl: './assign-plan-modal.component.html',
  styleUrls: ['./assign-plan-modal.component.scss']
})
export class AssignPlanModalComponent {
  @Input() agentId!: string;
  @Input() showModal = false;
  @Output() modalClosed = new EventEmitter<void>();
  @Output() planAssigned = new EventEmitter<void>();

  useTemplate = false;
  selectedTemplateId: string | null = null;
  templates: { label: string; value: string }[] = [];
  planForm: FormGroup;
  submitting = false;

  stepActionOptions = Object.values(StepAction).map(action => ({
    label: this.formatActionLabel(action),
    value: action
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
      steps: this.fb.array([], Validators.required)
    });
  }

  ngOnInit() {
    this.loadTemplates();
  }

  loadTemplates() {
    this.templatesService.list('', 0, 100).subscribe({
      next: (response) => {
        this.templates = response.content.map(template => ({
          label: template.name,
          value: template.id
        }));
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: `Failed to load templates: ${err.message}`
        });
      }
    });
  }

  toggleMode() {
    this.selectedTemplateId = null;
    this.planForm.reset();
    this.clearSteps();
  }

  get stepsFormArray(): FormArray {
    return this.planForm.get('steps') as FormArray;
  }

  addStep() {
    const stepGroup = this.fb.group({
      action: [null, Validators.required]
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
    if (this.submitting) return;

    const request: AssignPlanRequest = {
      useTemplate: this.useTemplate,
      templateId: this.useTemplate ? this.selectedTemplateId || undefined : undefined,
      plan: !this.useTemplate ? this.buildPlanFromForm() : undefined
    };

    this.submitting = true;
    this.agentsService.assignPlan(this.agentId, request).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Plan assigned successfully'
        });
        this.closeModal();
        this.planAssigned.emit();
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: `Failed to assign plan: ${err.message}`
        });
      }
    }).add(() => {
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
        logs: []
      }))
    };
  }

  closeModal() {
    this.showModal = false;
    this.modalClosed.emit();
  }

  private formatActionLabel(action: StepAction): string {
    return action.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  }
}