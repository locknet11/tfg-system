import { Component, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MenuItem } from 'primeng/api';
import { LayoutService } from 'src/app/shared/services/layout.service';
import { Router, RouterModule } from '@angular/router';
import { MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { AccountService } from 'src/app/shared/services/account.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterModule, MenuModule, ButtonModule],
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss'],
})
export class TopbarComponent {
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
