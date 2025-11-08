import { Component, ElementRef, ViewChild, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MenuItem } from 'primeng/api';
import { LayoutService } from 'src/app/shared/services/layout.service';
import { Router, RouterModule } from '@angular/router';
import { MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { AccountService } from 'src/app/shared/services/account.service';
import { OrganizationService } from 'src/app/pages/project-selector/data-access/organization.service';
import { ProjectService } from 'src/app/pages/project-selector/data-access/project.service';
import { OrganizationInfo } from 'src/app/pages/project-selector/data-access/organizations.model';
import { ProjectInfo } from 'src/app/pages/project-selector/data-access/projects.model';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MenuModule,
    ButtonModule,
    OverlayPanelModule,
  ],
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss'],
})
export class TopbarComponent implements OnInit {
  items: MenuItem[] = [
    {
      label: $localize`Logout`,
      icon: 'pi pi-sign-out',
      command: () => this.handleLogout(),
    },
  ];

  $accountInfo = this.accountService.$accountData;

  @ViewChild('menubutton') menuButton!: ElementRef;
  @ViewChild('topbarmenu') menu!: ElementRef;
  @ViewChild('selectorPanel') selectorPanel!: ElementRef;

  organizations: OrganizationInfo[] = [];
  projects: ProjectInfo[] = [];
  selectedOrganization: OrganizationInfo | null = null;
  selectedProject: ProjectInfo | null = null;

  selectorMenuItems: MenuItem[] = [];

  ngOnInit(): void {
    this.loadOrganizations();
    this.loadSelectedContext();
  }

  loadOrganizations(): void {
    this.organizationService.getOrganizations().subscribe({
      next: orgs => {
        this.organizations = orgs;
        this.loadProjects();
        this.buildSelectorMenu();
      },
      error: () => {
        // Handle error silently or show toast
      },
    });
  }

  loadProjects(): void {
    this.projectService.getProjects().subscribe({
      next: projs => {
        this.projects = projs;
        this.buildSelectorMenu();
      },
      error: () => {
        // Handle error
      },
    });
  }

  loadSelectedContext(): void {
    const orgStr = localStorage.getItem('selectedOrganization');
    const projStr = localStorage.getItem('selectedProject');
    if (orgStr) {
      this.selectedOrganization = JSON.parse(orgStr);
    }
    if (projStr) {
      this.selectedProject = JSON.parse(projStr);
    }
  }

  saveSelectedContext(): void {
    if (this.selectedOrganization) {
      localStorage.setItem(
        'selectedOrganization',
        JSON.stringify(this.selectedOrganization)
      );
    }
    if (this.selectedProject) {
      localStorage.setItem(
        'selectedProject',
        JSON.stringify(this.selectedProject)
      );
    }
  }

  buildSelectorMenu(): void {
    const items: MenuItem[] = [];
    this.organizations.forEach(org => {
      const orgProjects = this.projects.filter(
        p => p.organizationId === org.id
      );
      const subItems: MenuItem[] = orgProjects.map(proj => ({
        label: proj.name,
        command: () => this.selectProject(org, proj),
      }));
      items.push({
        label: org.name,
        items: subItems,
      });
    });
    items.push({
      separator: true,
    });
    items.push({
      label: $localize`Manage Organizations`,
      icon: 'pi pi-cog',
      command: () => this.goToProjectSelector(),
    });
    this.selectorMenuItems = items;
  }

  selectProject(org: OrganizationInfo, proj: ProjectInfo): void {
    this.selectedOrganization = org;
    this.selectedProject = proj;
    this.saveSelectedContext();
    // Optionally refresh or notify other components
  }

  goToProjectSelector(): void {
    this.router.navigate(['/project-selector']);
  }

  getCurrentSelectionLabel(): string {
    debugger;
    if (this.selectedOrganization && this.selectedProject) {
      return `${this.selectedOrganization.name} / ${this.selectedProject.name}`;
    }
    return $localize`Select Organization / Project`;
  }

  handleLogout() {
    this.accountService.logout();
    this.router.navigate(['login']);
  }

  constructor(
    public layoutService: LayoutService,
    private router: Router,
    private accountService: AccountService,
    private organizationService: OrganizationService,
    private projectService: ProjectService
  ) {}
}
