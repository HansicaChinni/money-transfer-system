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

  constructor(private fb: FormBuilder, private adminService: AdminService, private router: Router) {
    this.createForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      holderName: ['', [Validators.required]],
      initialBalance: [0, [Validators.required, Validators.min(0)]]
    });
  }

  onSubmit(): void {
    if (this.createForm.invalid) return;
    this.loading = true;
    this.errorMessage = '';
    this.adminService.createAccount(this.createForm.value).subscribe({
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
