import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableLazyLoadEvent, TableModule, TablePageEvent } from 'primeng/table';
import { User } from '../data-access/users.model';
import { UsersService } from '../data-access/users.service';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { Router, RouterModule } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { ToastService } from 'src/app/shared/services/toast.service';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    RouterModule,
    ConfirmDialogModule,
  ],
  providers: [UsersService, ConfirmationService],
  templateUrl: './users.component.html',
})
export class UsersComponent implements OnInit {
  users: User[] = [];
  totalRecords!: number;
  rows!: number;

  ngOnInit(): void {
    //this.getUsers();
  }

  getUsers(pageEvent?: TableLazyLoadEvent) {
    let page = 0;
    let size = 10;
    if (pageEvent && pageEvent.first && pageEvent.rows) {
      page = pageEvent.first / pageEvent.rows;
      size = pageEvent.rows;
    }
    this.usersService
      .getUsers(page, size)
      .pipe(
        map(res => {
          res.content = res.content.map(user => {
            if (user.role === 'ADMIN') {
              user.role = $localize`Administrator`;
            }

            if (user.role === 'USER') {
              user.role = $localize`User`;
            }
            return user;
          });
          return res;
        })
      )
      .subscribe({
        next: res => {
          this.users = res.content;
          this.totalRecords = res.totalElements;
          this.rows = res.size;
        },
      });
  }

  deleteConfirm(event: Event, userId: string) {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: $localize`Do you want to delete this user?`,
      header: $localize`Delete user`,
      icon: 'pi pi-info-circle',
      acceptButtonStyleClass: 'p-button-danger p-button-text',
      rejectButtonStyleClass: 'p-button-text p-button-text',
      acceptIcon: 'none',
      rejectIcon: 'none',
      accept: () => {
        this.delete(userId);
      },
      reject: () => {},
    });
  }

  editUser(userId: string) {
    this.router.navigate(['users', 'edit', userId]);
  }

  private delete(userId: string) {
    this.usersService.deleteUser(userId).subscribe({
      next: res => {
        this.toastService.success($localize`User deleted successfully`);
        this.getUsers();
      },
      error: err => {
        console.error(err);
      },
    });
  }

  constructor(
    private usersService: UsersService,
    private confirmationService: ConfirmationService,
    private toastService: ToastService,
    private router: Router
  ) {}
}
