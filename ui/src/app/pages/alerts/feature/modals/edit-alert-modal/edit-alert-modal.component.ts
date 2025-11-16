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
import {
  AlertConfiguration,
  getAllWhenConditions,
} from '../../../data-access/alerts.model';

@Component({
  selector: 'app-edit-alert-modal',
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
  templateUrl: './edit-alert-modal.component.html',
  styleUrls: ['./edit-alert-modal.component.scss'],
})
export class EditAlertModalComponent {
  private fb = inject(FormBuilder);
  private alertsService = inject(AlertsService);
  private messages = inject(MessageService);

  @Output() alertUpdated = new EventEmitter<void>();

  visibleSig = signal(false);
  currentAlertId = signal<string>('');
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

  show(alert: AlertConfiguration) {
    this.currentAlertId.set(alert.id);
    this.form.patchValue({
      sendTo: alert.sendTo,
      conditions: alert.conditions,
      enabled: alert.enabled,
    });
    this.visibleSig.set(true);
  }

  hide() {
    this.visibleSig.set(false);
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.alertsService
      .update(this.currentAlertId(), this.form.value)
      .subscribe({
        next: () => {
          this.messages.add({
            severity: 'success',
            summary: $localize`Success`,
            detail: $localize`Alert updated successfully`,
          });
          this.alertUpdated.emit();
          this.hide();
        },
        error: () => {
          this.messages.add({
            severity: 'error',
            summary: $localize`Error`,
            detail: $localize`Failed to update alert`,
          });
        },
      });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }
}
