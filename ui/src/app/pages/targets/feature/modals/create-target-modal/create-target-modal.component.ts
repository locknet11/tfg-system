import { CommonModule } from '@angular/common';
import { Component, signal, output, viewChild } from '@angular/core';
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
  CreateTargetRequest,
  OperatingSystem,
  TargetInfo,
} from '../../../data-access/targets.model';
import { ToastService } from 'src/app/shared/services/toast.service';
import { AgentSetupModalComponent } from '../agent-setup-modal/agent-setup-modal.component';

@Component({
  selector: 'app-create-target-modal',
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
    AgentSetupModalComponent,
  ],
  providers: [TargetsService],
  templateUrl: './create-target-modal.component.html',
  styleUrls: ['./create-target-modal.component.scss'],
})
export class CreateTargetModalComponent {
  visible = signal(false);
  targetCreated = output<TargetInfo>();
  
  agentSetupModal = viewChild.required(AgentSetupModalComponent);

  createTargetForm: FormGroup = this.fb.nonNullable.group({
    systemName: ['', [Validators.required]],
    description: [''],
    os: [OperatingSystem.LINUX, [Validators.required]],
  });

  operatingSystems: SelectItem[] = [
    { label: 'Linux', value: OperatingSystem.LINUX },
  ];

  constructor(
    private fb: FormBuilder,
    private targetsService: TargetsService,
    private toastService: ToastService
  ) {}

  show() {
    this.visible.set(true);
    this.createTargetForm.reset({
      systemName: '',
      description: '',
      os: OperatingSystem.LINUX,
    });
  }

  hide() {
    this.visible.set(false);
    this.createTargetForm.reset();
  }

  submit() {
    if (this.createTargetForm.valid) {
      // Get selected project from localStorage
      const selectedProjectStr = localStorage.getItem('selectedProject');
      const selectedOrgStr = localStorage.getItem('selectedOrganization');
      
      if (!selectedProjectStr || !selectedOrgStr) {
        this.toastService.error($localize`Please select an organization and project first`);
        return;
      }

      const selectedProject = JSON.parse(selectedProjectStr);
      const selectedOrg = JSON.parse(selectedOrgStr);

      const { systemName, description, os } = this.createTargetForm.getRawValue();
      const request: CreateTargetRequest = {
        systemName,
        description,
        os,
        projectId: selectedProject.id,
      };

      this.targetsService.createTarget(request).subscribe({
        next: res => {
          this.toastService.success($localize`Target created successfully`);
          this.targetCreated.emit(res);
          this.hide();
          
          // Show agent setup modal with the generated URL using short identifiers
          this.agentSetupModal().show(selectedOrg.organizationIdentifier, selectedProject.projectIdentifier, res.uniqueId);
        },
        error: err => {
          this.toastService.error($localize`Error creating target`);
        },
      });
    }
  }
}