import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Output,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { CheckboxModule } from 'primeng/checkbox';
import { MessageService } from 'primeng/api';

import { AlertsService } from '../../../data-access/alerts.service';
import { getAllWhenConditions } from '../../../data-access/alerts.model';

@Component({
  selector: 'app-create-alert-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    InputTextModule,
    ButtonModule,
    MultiSelectModule,
    CheckboxModule,
  ],
  templateUrl: './create-alert-modal.component.html',
  styleUrls: ['./create-alert-modal.component.scss'],
})
export class CreateAlertModalComponent {
  private fb = inject(FormBuilder);
  private alertsService = inject(AlertsService);
  private messages = inject(MessageService);

  @Output() alertCreated = new EventEmitter<void>();

  visibleSig = signal(false);
  form!: FormGroup;
  conditionOptions = getAllWhenConditions();

  ngOnInit() {
    this.initForm();
  }

  initForm() {
    this.form = this.fb.group({
      sendTo: ['', [Validators.required, Validators.email]],
      conditions: [[], [Validators.required]],
      enabled: [true],
    });
  }

  show() {
    this.visibleSig.set(true);
    this.form.reset({ enabled: true });
  }

  hide() {
    this.visibleSig.set(false);
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.alertsService.create(this.form.value).subscribe({
      next: () => {
        this.messages.add({
          severity: 'success',
          summary: $localize`Success`,
          detail: $localize`Alert created successfully`,
        });
        this.alertCreated.emit();
        this.hide();
      },
      error: () => {
        this.messages.add({
          severity: 'error',
          summary: $localize`Error`,
          detail: $localize`Failed to create alert`,
        });
      },
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }
}
