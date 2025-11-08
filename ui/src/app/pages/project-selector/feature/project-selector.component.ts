import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { StepsModule } from 'primeng/steps';
import { MenuItem } from 'primeng/api';
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
import {
  Organization,
  OrganizationInfo,
  CreateOrganizationRequest,
} from '../data-access/organizations.model';
import {
  Project,
  ProjectInfo,
  CreateProjectRequest,
} from '../data-access/projects.model';

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
    StepsModule,
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
  organizationDropdownOptions: (
    | OrganizationInfo
    | { id: string; name: string; isCreateOption: boolean }
  )[] = [];
  projectDropdownOptions: (
    | ProjectInfo
    | { id: string; name: string; isCreateOption: boolean }
  )[] = [];

  // Dialog states
  showCreateOrganizationDialog = false;
  showCreateProjectDialog = false;
  showSetupWizardDialog = false;

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

  // Setup wizard properties
  setupWizardActiveIndex = 0;
  setupWizardItems: MenuItem[] = [
    { label: 'Organization' },
    { label: 'Project' },
  ];

  loading = false;
  showNoOrganizationsMessage = false;

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
      if (org && (org as any).isCreateOption) {
        // Reset the dropdown and open create organization dialog
        this.selectorForm.get('organization')?.setValue(null);
        this.openCreateOrganizationDialog();
        return;
      }

      if (org) {
        this.filteredProjects = this.allProjects.filter(
          p => p.organizationId === org.id
        );
        this.updateProjectDropdownOptions();
        this.selectorForm.get('project')?.setValue(null);
        this.selectorForm.get('project')?.enable();
      } else {
        this.filteredProjects = [];
        this.projectDropdownOptions = [];
        this.selectorForm.get('project')?.setValue(null);
        this.selectorForm.get('project')?.disable();
      }
    });

    // Handle project selection including create new option
    this.selectorForm.get('project')?.valueChanges.subscribe(project => {
      if (project && (project as any).isCreateOption) {
        // Reset the dropdown and open create project dialog
        this.selectorForm.get('project')?.setValue(null);
        this.openCreateProjectDialog();
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

        // Show suggestion message if no organizations exist
        this.showNoOrganizationsMessage = this.organizations.length === 0;

        // Update organization dropdown options
        this.updateOrganizationDropdownOptions();

        if (this.organizations.length > 0) {
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
        this.updateProjectDropdownOptions();
      },
      error: (error) => {
        this.toastService.error($localize`Error loading projects`);
      }
    });
  }

  updateOrganizationDropdownOptions(): void {
    this.organizationDropdownOptions = [
      ...this.organizations,
      { id: 'create-new', name: 'Create new organization...', isCreateOption: true }
    ];
  }

  updateProjectDropdownOptions(): void {
    const currentOrg = this.selectorForm.get('organization')?.value;
    if (currentOrg) {
      const orgProjects = this.allProjects.filter(p => p.organizationId === currentOrg.id);
      this.projectDropdownOptions = [
        ...orgProjects,
        { id: 'create-new', name: 'Create new project...', isCreateOption: true }
      ];
    } else {
      this.projectDropdownOptions = [];
    }
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
        this.updateOrganizationDropdownOptions();
        this.showNoOrganizationsMessage = false;
        this.toastService.success($localize`Organization created successfully`);
        this.closeCreateOrganizationDialog();

        // Auto-select the created organization
        this.selectorForm.patchValue({
          organization: organization
        });
      },
      error: (error) => {
        this.toastService.error(error.error?.message || $localize`Error creating organization`);
      }
    });
  }

  // Project dialog methods
  openCreateProjectDialog(): void {
    const currentOrg = this.selectorForm.get('organization')?.value;
    this.createProjectForm.reset();
    if (currentOrg) {
      this.createProjectForm.patchValue({
        organizationId: currentOrg.id
      });
    }
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
        this.updateProjectDropdownOptions();
        this.toastService.success($localize`Project created successfully`);
        this.closeCreateProjectDialog();
      },
      error: (error) => {
        this.toastService.error(error.error?.message || $localize`Error creating project`);
      }
    });
  }

  // Setup wizard methods
  openSetupWizard(): void {
    this.setupWizardActiveIndex = 0;
    this.createOrganizationForm.reset();
    this.createProjectForm.reset();
    this.showSetupWizardDialog = true;
  }

  closeSetupWizard(): void {
    this.showSetupWizardDialog = false;
    this.setupWizardActiveIndex = 0;
    this.createOrganizationForm.reset();
    this.createProjectForm.reset();
  }

  nextStep(): void {
    if (this.setupWizardActiveIndex === 0) {
      // Validate organization form
      if (this.createOrganizationForm.invalid) return;

      // Create organization
      this.createOrganizationInWizard();
    }
  }

  previousStep(): void {
    if (this.setupWizardActiveIndex > 0) {
      this.setupWizardActiveIndex--;
    }
  }

  createOrganizationInWizard(): void {
    const formData = this.createOrganizationForm.getRawValue();
    const request: CreateOrganizationRequest = {
      name: formData.name,
      description: formData.description || undefined,
    };

    this.organizationService.createOrganization(request).subscribe({
      next: (organization) => {
        this.organizations.push(organization);
        this.showNoOrganizationsMessage = false;
        this.toastService.success($localize`Organization created successfully`);

        // Move to next step and set the organization in project form
        this.setupWizardActiveIndex = 1;
        this.createProjectForm.patchValue({
          organizationId: organization.id
        });
      },
      error: (error) => {
        this.toastService.error(error.error?.message || $localize`Error creating organization`);
      }
    });
  }

  createProjectInWizard(): void {
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
        this.updateProjectDropdownOptions();
        this.toastService.success($localize`Project created successfully`);
        this.closeSetupWizard();

        // Auto-select the created organization and project
        this.selectorForm.patchValue({
          organization: this.organizations.find(org => org.id === project.organizationId),
          project: project
        });
      },
      error: (error) => {
        this.toastService.error(error.error?.message || $localize`Error creating project`);
      }
    });
  }
}