import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { FormErrorDirective } from 'src/app/shared/directives/form-error.directive';
import { ToastService } from 'src/app/shared/services/toast.service';
import { Router } from '@angular/router';
import { MessageModule } from 'primeng/message';
import { AuthenticationService } from '../../data-access/authentication.service';
import { AccountService } from 'src/app/shared/services/account.service';

@Component({
  selector: 'app-initial-setup',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    ReactiveFormsModule,
    FormsModule,
    FormErrorDirective,
    MessageModule,
  ],
  templateUrl: './initial-setup.component.html',
  styleUrls: ['./initial-setup.component.scss'],
})
export class InitialSetupComponent {
  constructor(
    private fb: FormBuilder,
    private toastService: ToastService,
    private router: Router,
    private authService: AuthenticationService,
    private accountService: AccountService
  ) {}

  setupForm: FormGroup = this.fb.nonNullable.group(
    {
      fullName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: this.passwordMatchValidator }
  );

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const formGroup = control as FormGroup;
    const password = formGroup.get('password')?.value;
    const confirmPassword = formGroup.get('confirmPassword')?.value;

    if (password !== confirmPassword) {
      formGroup.get('confirmPassword')?.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }

    return null;
  }

  setupAdministrator() {
    if (this.setupForm.invalid) return;

    const formData = this.setupForm.getRawValue();

    this.authService
      .setup({
        fullName: formData.fullName,
        email: formData.email,
        password: formData.password,
      })
      .subscribe({
        next: response => {
          this.authService.saveToken(response.token);
          this.accountService.refreshAccountInfo();
          this.toastService.success(
            $localize`Administrator configured successfully`
          );
          this.router.navigate(['/']);
        },
        error: error => {
          this.toastService.error(
            error.error?.message || $localize`Error configuring administrator`
          );
        },
      });
  }

  markErrors(formControl: string) {
    if (this.setupForm.controls[formControl].errors) {
      return 'ng-dirty ng-invalid';
    }

    return '';
  }
}
