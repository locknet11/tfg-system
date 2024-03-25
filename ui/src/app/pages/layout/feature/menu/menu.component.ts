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
    this.accountService.$accountData.pipe(take(1)).subscribe({
      next: account => {
        this.model = [
          {
            label: $localize`Home`,
            items: [
              {
                label: $localize`Main page`,
                icon: 'pi pi-home',
                routerLink: ['mainpage'],
              },
            ],
          },
          {
            label: $localize`Settings`,
            items: [
              {
                label: $localize`Users`,
                icon: 'pi pi-fw pi-box',
                routerLink: ['users'],
              },
            ],
          },
        ];
      },
    });
  }
}
