import { CommonModule } from '@angular/common';
import { Component, signal, output, inject } from '@angular/core';
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
} from '../../../data-access/templates.model';
import { TemplatesService } from '../../../data-access/templates.service';
import { ToastService } from 'src/app/shared/services/toast.service';

@Component({
  selector: 'app-create-template-modal',
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
  templateUrl: './create-template-modal.component.html',
  styleUrls: ['./create-template-modal.component.scss'],
})
export class CreateTemplateModalComponent {
  private fb = inject(FormBuilder);
  private templatesService = inject(TemplatesService);
  private toastService = inject(ToastService);

  visible = signal(false);
  templateCreated = output();

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

  show() {
    this.visible.set(true);
    this.form.reset({
      name: '',
      description: '',
      plan: {
        notes: '',
        steps: [],
      },
    });
    this.addStep();
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
    if (this.form.invalid) return;
    const value = this.form.value;
    const template = {
      name: value.name!,
      description: value.description!,
      plan: {
        notes: value.plan!.notes!,
        steps: value.plan!.steps!.map((s: any) => ({ action: s.action })),
      },
    };
    this.templatesService.create(template).subscribe({
      next: () => {
        this.toastService.success($localize`Template created successfully`);
        this.templateCreated.emit();
        this.hide();
      },
      error: () => {
        this.toastService.error($localize`Error creating template`);
      },
    });
  }

  resetForm() {
    this.form.reset();
    this.steps.clear();
  }
}
