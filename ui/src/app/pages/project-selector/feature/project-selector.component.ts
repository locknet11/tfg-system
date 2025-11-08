import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ToastService } from 'src/app/shared/services/toast.service';
import { Router } from '@angular/router';

interface Organization {
  id: string;
  name: string;
}

interface Project {
  id: string;
  name: string;
  organizationId: string;
}

@Component({
  selector: 'app-project-selector',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    DropdownModule,
    ReactiveFormsModule,
    FormsModule,
  ],
  templateUrl: './project-selector.component.html',
  styles: ['.auth-box { box-shadow: 0 0 20px rgba(255, 255, 255, 0.1); }'],
})
export class ProjectSelectorComponent implements OnInit {
  organizations: Organization[] = [
    { id: '1', name: 'Acme Corporation' },
    { id: '2', name: 'Tech Innovations Inc.' },
    { id: '3', name: 'Global Solutions Ltd.' },
  ];

  allProjects: Project[] = [
    { id: '1', name: 'Website Redesign', organizationId: '1' },
    { id: '2', name: 'Mobile App Development', organizationId: '1' },
    { id: '3', name: 'Cloud Migration', organizationId: '1' },
    { id: '4', name: 'AI Research Project', organizationId: '2' },
    { id: '5', name: 'Data Analytics Platform', organizationId: '2' },
    { id: '6', name: 'Security Audit', organizationId: '3' },
    { id: '7', name: 'Infrastructure Upgrade', organizationId: '3' },
  ];

  filteredProjects: Project[] = [];

  selectorForm: FormGroup = this.fb.nonNullable.group({
    organization: [null, [Validators.required]],
    project: [null, [Validators.required]],
  });

  constructor(
    private fb: FormBuilder,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.selectorForm.get('organization')?.valueChanges.subscribe(org => {
      if (org) {
        this.filteredProjects = this.allProjects.filter(
          p => p.organizationId === org.id
        );
        this.selectorForm.get('project')?.setValue(null);
        this.selectorForm.get('project')?.enable();
      } else {
        this.filteredProjects = [];
        this.selectorForm.get('project')?.setValue(null);
        this.selectorForm.get('project')?.disable();
      }
    });

    this.selectorForm.get('project')?.disable();
  }

  enterProject() {
    if (this.selectorForm.invalid) return;

    const formData = this.selectorForm.getRawValue();
    this.toastService.success(
      $localize`Entering project: ${formData.project.name}`
    );
    this.router.navigate(['dashboard']);
  }
}
