import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth-service.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './change-password-component.component.html',
  styleUrls: ['./change-password-component.component.css']
})
export class ChangePasswordComponent {
  form: FormGroup;
  loading = false;
  errorMessage = '';
  successMessage = '';

  hideCurrent = true;
  hideNew = true;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService
  ) {
    this.form = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.changePassword(this.form.value).subscribe({
      next: (res) => {
        this.loading = false;
        this.successMessage = res.message || 'Password updated successfully. Please login again.';
        this.form.reset();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Unable to update password.';
      }
    });
  }

  toggleCurrent(): void {
    this.hideCurrent = !this.hideCurrent;
  }

  toggleNew(): void {
    this.hideNew = !this.hideNew;
  }
}

