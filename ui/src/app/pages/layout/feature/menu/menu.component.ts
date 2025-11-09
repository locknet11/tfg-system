import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LayoutService } from 'src/app/shared/services/layout.service';
import { MenuitemComponent } from '../menuitem/menuitem.component';
import { AccountService } from 'src/app/shared/services/account.service';
import { take } from 'rxjs';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, MenuitemComponent],
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.scss'],
})
export class MenuComponent implements OnInit {
  model: any[] = [];

  constructor(
    public layoutService: LayoutService,
    private accountService: AccountService
  ) {}

  ngOnInit() {
    this.model = [
      {
        items: [
          {
            label: $localize`Dashboard`,
            icon: 'pi pi-home',
            routerLink: ['dashboard'],
          },
          {
            label: $localize`Targets`,
            icon: 'pi pi-flag',
            routerLink: ['targets'],
          },
          {
            label: $localize`Agents`,
            icon: 'pi pi-users',
            routerLink: ['agents'],
          },
          {
            label: $localize`Templates`,
            icon: 'pi pi-file',
            routerLink: ['templates'],
          },
          {
            label: $localize`Alerts`,
            icon: 'pi pi-bell',
            routerLink: ['alerts'],
          },
          {
            label: $localize`Reports`,
            icon: 'pi pi-chart-bar',
            routerLink: ['reports'],
          },
        ],
      },
    ];
  }
}
