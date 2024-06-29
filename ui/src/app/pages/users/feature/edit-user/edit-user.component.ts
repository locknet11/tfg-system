import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { UsersService } from '../../data-access/users.service';
import { ToastService } from 'src/app/shared/services/toast.service';
import { AccountInfo } from 'src/app/shared/models/global.model';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { MultiSelectModule } from 'primeng/multiselect';
import { SelectItem } from 'primeng/api';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { AccountService } from 'src/app/shared/services/account.service';
import { moduleAccessList, userRoles } from '../../data-access/users.model';

@Component({
  selector: 'app-edit-user',
  standalone: true,
  imports: [
    CommonModule,
    DropdownModule,
    MultiSelectModule,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    ButtonModule,
    RouterModule,
  ],
  providers: [UsersService],
  templateUrl: './edit-user.component.html',
})
export class EditUserComponent {
  editUserForm: FormGroup = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    fullName: ['', [Validators.required]],
    role: ['', [Validators.required]],
    moduleAccess: ['', [Validators.required]],
  });

  user!: AccountInfo;
  userId!: string;

  roles: SelectItem[] = userRoles;

  modules: SelectItem[] = moduleAccessList;

  private getUserDetails() {
    this.userId = this.activeRoute.snapshot.params['id'];
    if (!this.userId) {
      this.router.navigate(['users']);
    }

    this.usersService.getUserById(this.userId).subscribe({
      next: res => {
        this.user = res;
      },
      error: err => {
        if (err.status === 404) {
          this.toastService.error($localize`User not found`);
        }
      },
    });
  }

  submit() {
    const { email, fullName, role, moduleAccess } =
      this.editUserForm.getRawValue();

    this.usersService
      .updateById({ email, fullName, role, moduleAccess }, this.userId)
      .subscribe({
        next: res => {
          this.accountService.refreshAccountInfo();
          this.toastService.success($localize`User updated successfully`);
          this.router.navigate(['users']);
        },
        error: err => {
          this.toastService.error(err);
        },
      });
  }

  constructor(
    private router: Router,
    private activeRoute: ActivatedRoute,
    private usersService: UsersService,
    private toastService: ToastService,
    private fb: FormBuilder,
    private accountService: AccountService
  ) {
    this.getUserDetails();
  }
}
