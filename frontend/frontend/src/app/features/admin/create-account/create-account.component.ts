import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-create-account',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './create-account.component.html',
  styleUrl: './create-account.component.scss'
})
export class CreateAccountComponent {
  createForm: FormGroup;
  loading = false;
  errorMessage = '';
  passwordVisible = false;

  togglePasswordVisibility(): void {
    this.passwordVisible = !this.passwordVisible;
  }

  constructor(private fb: FormBuilder, private adminService: AdminService, private router: Router) {
    this.createForm = this.fb.group({
      holderName: [
        '',
        [
          Validators.required,
          Validators.pattern(/^[A-Za-z\s]+$/)
        ]
      ],
      username: [
        '',
        [
          Validators.required,
          Validators.pattern(/^[A-Za-z0-9]+$/)
        ]
      ],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(
            /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).+$/
          )
        ]
      ],
      initialBalance: [
        '',
        [
          Validators.required,
          Validators.min(1000)
        ]
      ]
    });

  }

  onSubmit(): void {
    if (this.createForm.invalid) return;

    const formValue = {
      ...this.createForm.value,
      holderName: this.createForm.value.holderName.trim()
    };

    this.loading = true;
    this.errorMessage = '';

    this.adminService.createAccount(formValue).subscribe({
      next: (account) => {
        this.router.navigate(['/admin/accounts', account.id]);
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Failed to create account';
      }
    });
  }
}
