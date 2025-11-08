import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { AuthenticationService } from '../data-access/authentication.service';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { FormErrorDirective } from 'src/app/shared/directives/form-error.directive';
import { ToastService } from 'src/app/shared/services/toast.service';
import { Router } from '@angular/router';
import { MessageModule } from 'primeng/message';

@Component({
  selector: 'app-authentication',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    CheckboxModule,
    InputTextModule,
    ReactiveFormsModule,
    FormsModule,
    MessageModule,
  ],
  providers: [AuthenticationService],
  templateUrl: './authentication.component.html',
  styles: [
    '.auth-box { background-color: rgba(0, 0, 0, 0.4); box-shadow: 0 0 20px rgba(0, 0, 0, 0.7) ; backdrop-filter: blur(10px); }',
  ],
})
export class AuthenticationComponent {
  constructor(
    private fb: FormBuilder,
    private authService: AuthenticationService,
    private toastService: ToastService,
    private router: Router
  ) {}
  loginForm: FormGroup = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  failedAuth = false;

  login() {
    if (this.loginForm.invalid) return;
    const formData = this.loginForm.getRawValue();
    this.authService
      .login({
        username: formData.username,
        password: formData.password,
      })
      .subscribe({
        next: res => {
          this.authService.saveToken(res.token);
          this.router.navigate(['project-selector']);
        },
        error: err => {
          this.failedAuth = true;
        },
      });
  }

  markErrors(formControl: string) {
    if (this.loginForm.controls[formControl].errors) {
      return 'ng-dirty ng-invalid';
    }

    return '';
  }
}
