import { CommonModule } from '@angular/common';
import { Component, signal, output, inject, input } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { InputTextModule } from 'primeng/inputtext';

import {
  StepAction,
  stepActionLabel,
  PlanTemplate,
} from '../../../data-access/templates.model';
import { TemplatesService } from '../../../data-access/templates.service';
import { ToastService } from 'src/app/shared/services/toast.service';

@Component({
  selector: 'app-edit-template-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    InputTextareaModule,
    DropdownModule,
    DialogModule,
  ],
  templateUrl: './edit-template-modal.component.html',
  styleUrls: ['./edit-template-modal.component.scss'],
})
export class EditTemplateModalComponent {
  private fb = inject(FormBuilder);
  private templatesService = inject(TemplatesService);
  private toastService = inject(ToastService);

  visible = signal(false);
  templateEdited = output();
  template = input<PlanTemplate | null>(null);

  form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    plan: this.fb.group({
      notes: [''],
      steps: this.fb.array<FormGroup>([], Validators.minLength(1)),
    }),
  });

  stepActions = Object.values(StepAction).map(a => ({
    label: stepActionLabel(a),
    value: a,
  }));

  get steps(): FormArray {
    return this.form.get('plan.steps') as FormArray;
  }

  show(template: PlanTemplate) {
    this.visible.set(true);
    this.form.patchValue({
      name: template.name,
      description: template.description,
      plan: {
        notes: template.plan.notes,
      },
    });
    this.steps.clear();
    template.plan.steps.forEach(step => this.addStep(step.action));
  }

  hide() {
    this.visible.set(false);
    this.steps.clear();
    this.form.reset();
  }

  addStep(action: StepAction = StepAction.SYSTEM_SCAN) {
    this.steps.push(
      this.fb.group({
        action: [action, Validators.required],
      })
    );
  }

  removeStep(index: number) {
    this.steps.removeAt(index);
  }

  submit() {
    if (this.form.invalid || !this.template()) return;
    const value = this.form.value;
    const template = {
      name: value.name!,
      description: value.description!,
      plan: {
        notes: value.plan!.notes!,
        steps: value.plan!.steps!.map((s: any) => ({ action: s.action })),
      },
    };
    this.templatesService.update(this.template()!.id, template).subscribe({
      next: () => {
        this.toastService.success($localize`Template updated successfully`);
        this.templateEdited.emit();
        this.hide();
      },
      error: () => {
        this.toastService.error($localize`Error updating template`);
      },
    });
  }

  resetForm() {
    this.form.reset();
    this.steps.clear();
  }
}