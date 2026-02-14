import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { UserService } from '../../../core/services/user.service';
import { AccountResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  accountInfo: AccountResponse | null = null;
  passwordForm: FormGroup;
  loading = false;
  loadingAccount = true;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private userService: UserService
  ) {
    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(
            /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).+$/
          )
        ]
      ],
      confirmPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(
            /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).+$/
          )
        ]
      ]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.loadAccountInfo();
  }

  loadAccountInfo(): void {
    this.loadingAccount = true;
    this.userService.getBalance().subscribe({
      next: (account) => {
        this.accountInfo = account;
        this.loadingAccount = false;
      },
      error: (error) => {
        console.error('Failed to load account info', error);
        this.loadingAccount = false;
      }
    });
  }

  passwordMatchValidator(form: FormGroup) {
    const newPassword = form.get('newPassword');
    const confirmPassword = form.get('confirmPassword');
    
    if (newPassword && confirmPassword && newPassword.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  onChangePassword(): void {
    if (this.passwordForm.invalid) {
      return;
    }

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    const { currentPassword, newPassword } = this.passwordForm.value;

    this.userService.changePassword({ currentPassword, newPassword }).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = response.message || 'Password changed successfully!';
        this.passwordForm.reset();
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Failed to change password';
      }
    });
  }

  getStatusBadgeClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'ACTIVE':
        return 'badge-success';
      case 'LOCKED':
        return 'badge-warning';
      case 'CLOSED':
        return 'badge-danger';
      default:
        return 'badge-primary';
    }
  }
}
