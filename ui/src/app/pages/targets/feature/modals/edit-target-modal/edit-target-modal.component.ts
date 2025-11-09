import { CommonModule } from '@angular/common';
import { Component, signal, output } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { TargetsService } from '../../../data-access/targets.service';
import {
  TargetInfo,
  TargetStatus,
  OperatingSystem,
  UpdateTargetRequest,
} from '../../../data-access/targets.model';
import { ToastService } from 'src/app/shared/services/toast.service';

@Component({
  selector: 'app-edit-target-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    InputTextareaModule,
    DropdownModule,
    DialogModule,
  ],
  providers: [TargetsService],
  templateUrl: './edit-target-modal.component.html',
  styleUrls: ['./edit-target-modal.component.scss'],
})
export class EditTargetModalComponent {
  visible = signal(false);
  targetUpdated = output<void>();
  currentTargetId = signal<string | null>(null);

  editTargetForm: FormGroup = this.fb.nonNullable.group({
    systemName: ['', [Validators.required]],
    description: [''],
    os: [OperatingSystem.LINUX, [Validators.required]],
    ipOrDomain: [''],
    status: [TargetStatus.OFFLINE, [Validators.required]],
    assignedAgent: [''],
  });

  statuses: SelectItem[] = [
    { label: $localize`Online`, value: TargetStatus.ONLINE },
    { label: $localize`Offline`, value: TargetStatus.OFFLINE },
    { label: $localize`In Review`, value: TargetStatus.IN_REVIEW },
  ];

  operatingSystems: SelectItem[] = [
    { label: 'Linux', value: OperatingSystem.LINUX },
  ];

  constructor(
    private fb: FormBuilder,
    private targetsService: TargetsService,
    private toastService: ToastService
  ) {}

  show(target: TargetInfo) {
    this.currentTargetId.set(target.id);
    this.editTargetForm.patchValue({
      systemName: target.systemName,
      description: target.description,
      os: target.os,
      ipOrDomain: target.ipOrDomain,
      status: target.status,
      assignedAgent: target.assignedAgent,
    });
    this.visible.set(true);
  }

  hide() {
    this.visible.set(false);
    this.currentTargetId.set(null);
    this.editTargetForm.reset();
  }

  submit() {
    if (this.editTargetForm.valid && this.currentTargetId()) {
      const { systemName, description, os, ipOrDomain, status, assignedAgent } =
        this.editTargetForm.getRawValue();
      const request: UpdateTargetRequest = {
        systemName,
        description,
        os,
        ipOrDomain,
        status,
        assignedAgent,
      };

      this.targetsService
        .updateById(request, this.currentTargetId()!)
        .subscribe({
          next: res => {
            this.toastService.success($localize`Target updated successfully`);
            this.targetUpdated.emit();
            this.hide();
          },
          error: err => {
            this.toastService.error($localize`Error updating target`);
          },
        });
    }
  }
}