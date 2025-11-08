import { Component, ElementRef, ViewChild, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MenuItem } from 'primeng/api';
import { LayoutService } from 'src/app/shared/services/layout.service';
import { Router, RouterModule } from '@angular/router';
import { MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { AccountService } from 'src/app/shared/services/account.service';
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

  selectedOrganization: OrganizationInfo | null = null;
  selectedProject: ProjectInfo | null = null;

  selectorMenuItems: MenuItem[] = [
    {
      label: $localize`Switch Project`,
      icon: 'pi pi-sync',
      command: () => this.goToProjectSelector(),
    },
  ];

  ngOnInit(): void {
    this.loadSelectedContext();
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

  goToProjectSelector(): void {
    this.router.navigate(['/project-selector']);
  }

  getCurrentSelectionLabel(): string {
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
    private accountService: AccountService
  ) {}
}
