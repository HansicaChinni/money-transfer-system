import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { UserService } from '../../../core/services/user.service';
import { AccountResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './transfer.component.html',
  styleUrl: './transfer.component.scss'
})
export class TransferComponent implements OnInit {
  transferForm: FormGroup;
  loading = false;
  successMessage = '';
  errorMessage = '';
  accountInfo: AccountResponse | null = null;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private router: Router
  ) {
    this.transferForm = this.fb.group({
      toAccountId: ['', [Validators.required, Validators.min(1)]],
      amount: ['', [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit(): void {
    this.loadAccountInfo();
  }

  loadAccountInfo(): void {
    this.userService.getBalance().subscribe({
      next: (account) => {
        this.accountInfo = account;
      },
      error: (error) => {
        console.error('Failed to load account info', error);
      }
    });
  }

  onSubmit(): void {
    if (this.transferForm.invalid) {
      return;
    }

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.userService.transfer(this.transferForm.value).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.status === 'SUCCESS') {
          this.successMessage = `Transfer of $${response.amount} completed successfully! Transaction ID: ${response.transactionId}`;
          this.transferForm.reset();
          this.loadAccountInfo();
          setTimeout(() => {
            this.router.navigate(['/user/transactions']);
          }, 2000);
        } else {
          this.errorMessage = response.message || 'Transfer failed';
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Transfer failed. Please try again.';
      }
    });
  }
}
