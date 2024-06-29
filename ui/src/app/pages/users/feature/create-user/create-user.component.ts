import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { PasswordModule } from 'primeng/password';
import { UsersService } from '../../data-access/users.service';
import {
  CreateUserRequest,
  moduleAccessList,
  userRoles,
} from '../../data-access/users.model';
import { Router, RouterModule } from '@angular/router';
import { ToastService } from 'src/app/shared/services/toast.service';

@Component({
  selector: 'app-create-user',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    MultiSelectModule,
    PasswordModule,
    RouterModule,
  ],
  providers: [UsersService],
  templateUrl: './create-user.component.html',
})
export class CreateUserComponent {
  createUserForm: FormGroup = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    fullName: ['', [Validators.required]],
    role: ['USER', [Validators.required]],
    moduleAccess: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    passwordConfirmation: ['', [Validators.required]],
  });

  roles: SelectItem[] = userRoles;

  modules: SelectItem[] = moduleAccessList;

  submit() {
    let { email, role, password, fullName, moduleAccess } =
      this.createUserForm.getRawValue();
    const request: CreateUserRequest = {
      email,
      role,
      password,
      fullName,
      moduleAccess,
    };

    this.userService.createUser(request).subscribe({
      next: res => {
        this.toastService.success($localize`User created successfully`);
        this.router.navigate(['users']);
      },
      error: err => {
        this.toastService.error(err);
      },
    });
  }

  get isValidPassword() {
    let password = this.createUserForm.get('password')?.value;
    let passwordConfirmation = this.createUserForm.get(
      'passwordConfirmation'
    )?.value;
    return password == passwordConfirmation;
  }

  constructor(
    private fb: FormBuilder,
    private userService: UsersService,
    private router: Router,
    private toastService: ToastService
  ) {}
}
