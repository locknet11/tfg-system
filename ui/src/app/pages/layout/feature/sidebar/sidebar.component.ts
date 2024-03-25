import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { MenuModule } from 'primeng/menu';
import { AccountService } from 'src/app/shared/services/account.service';
import { LayoutService } from 'src/app/shared/services/layout.service';
import { MenuComponent } from '../menu/menu.component';
import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, MenuComponent, MenuModule, ButtonModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
})
export class SidebarComponent {
  @HostListener('window:resize') resize() {
    this.isMobile = this.layoutService.isMobile();
  }

  isMobile = this.layoutService.isMobile();
  items: MenuItem[] = [
    {
      label: $localize`Logout`,
      icon: 'pi pi-sign-out',
      command: () => this.handleLogout(),
    },
  ];

  $accountInfo = this.accountService.$accountData;

  handleLogout() {
    this.accountService.logout();
    this.router.navigate(['login']);
  }

  constructor(
    public layoutService: LayoutService,
    private accountService: AccountService,
    private router: Router,
    public el: ElementRef
  ) {}
}
