import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ToastService } from 'src/app/shared/services/toast.service';
import { Router } from '@angular/router';
import { OrganizationService } from '../data-access/organization.service';
import { ProjectService } from '../data-access/project.service';
import { Organization, OrganizationInfo, CreateOrganizationRequest } from '../data-access/organizations.model';
import { Project, ProjectInfo, CreateProjectRequest } from '../data-access/projects.model';

@Component({
  selector: 'app-project-selector',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    DropdownModule,
    DialogModule,
    InputTextModule,
    InputTextareaModule,
    ReactiveFormsModule,
    FormsModule,
  ],
  templateUrl: './project-selector.component.html',
  styles: ['.auth-box { box-shadow: 0 0 20px rgba(255, 255, 255, 0.1); }'],
})
export class ProjectSelectorComponent implements OnInit {
  organizations: OrganizationInfo[] = [];
  allProjects: ProjectInfo[] = [];
  filteredProjects: ProjectInfo[] = [];

  // Dialog states
  showCreateOrganizationDialog = false;
  showCreateProjectDialog = false;

  // Forms
  selectorForm: FormGroup = this.fb.nonNullable.group({
    organization: [null, [Validators.required]],
    project: [null, [Validators.required]],
  });

  createOrganizationForm: FormGroup = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    description: [''],
  });

  createProjectForm: FormGroup = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    description: [''],
    organizationId: [null, [Validators.required]],
  });

  loading = false;

  constructor(
    private fb: FormBuilder,
    private toastService: ToastService,
    private router: Router,
    private organizationService: OrganizationService,
    private projectService: ProjectService
  ) {}

  ngOnInit(): void {
    this.loadOrganizations();

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

  loadOrganizations(): void {
    this.loading = true;
    this.organizationService.getOrganizations().subscribe({
      next: (organizations) => {
        this.organizations = organizations;
        this.loading = false;

        // If no organizations, force creation
        if (this.organizations.length === 0) {
          this.showCreateOrganizationDialog = true;
        } else {
          this.loadProjects();
        }
      },
      error: (error) => {
        this.loading = false;
        this.toastService.error($localize`Error loading organizations`);
      }
    });
  }

  loadProjects(): void {
    this.projectService.getProjects().subscribe({
      next: (projects) => {
        this.allProjects = projects;
      },
      error: (error) => {
        this.toastService.error($localize`Error loading projects`);
      }
    });
  }

  enterProject() {
    if (this.selectorForm.invalid) return;

    const formData = this.selectorForm.getRawValue();
    this.toastService.success(
      $localize`Entering project: ${formData.project.name}`
    );
    this.router.navigate(['dashboard']);
  }

  // Organization dialog methods
  openCreateOrganizationDialog(): void {
    this.createOrganizationForm.reset();
    this.showCreateOrganizationDialog = true;
  }

  closeCreateOrganizationDialog(): void {
    this.showCreateOrganizationDialog = false;
    this.createOrganizationForm.reset();
  }

  createOrganization(): void {
    if (this.createOrganizationForm.invalid) return;

    const formData = this.createOrganizationForm.getRawValue();
    const request: CreateOrganizationRequest = {
      name: formData.name,
      description: formData.description || undefined,
    };

    this.organizationService.createOrganization(request).subscribe({
      next: (organization) => {
        this.organizations.push(organization);
        this.toastService.success($localize`Organization created successfully`);
        this.closeCreateOrganizationDialog();

        // If this was the first organization, load projects
        if (this.organizations.length === 1) {
          this.loadProjects();
        }
      },
      error: (error) => {
        this.toastService.error(error.error?.message || $localize`Error creating organization`);
      }
    });
  }

  // Project dialog methods
  openCreateProjectDialog(): void {
    this.createProjectForm.reset();
    this.showCreateProjectDialog = true;
  }

  closeCreateProjectDialog(): void {
    this.showCreateProjectDialog = false;
    this.createProjectForm.reset();
  }

  createProject(): void {
    if (this.createProjectForm.invalid) return;

    const formData = this.createProjectForm.getRawValue();
    const request: CreateProjectRequest = {
      name: formData.name,
      description: formData.description || undefined,
      organizationId: formData.organizationId,
    };

    this.projectService.createProject(request).subscribe({
      next: (project) => {
        this.allProjects.push(project);
        // Update filtered projects if the current organization is selected
        const currentOrg = this.selectorForm.get('organization')?.value;
        if (currentOrg && currentOrg.id === project.organizationId) {
          this.filteredProjects.push(project);
        }
        this.toastService.success($localize`Project created successfully`);
        this.closeCreateProjectDialog();
      },
      error: (error) => {
        this.toastService.error(error.error?.message || $localize`Error creating project`);
      }
    });
  }
}
