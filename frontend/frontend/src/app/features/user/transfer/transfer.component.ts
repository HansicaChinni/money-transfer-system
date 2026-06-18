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
  confirming = false;
  successMessage = '';
  errorMessage = '';
  accountInfo: AccountResponse | null = null;
  recipientInfo: AccountResponse | null = null;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private router: Router
  ) {
    this.transferForm = this.fb.group({
      toAccountId: ['', [Validators.required, Validators.min(1)]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      notes: ['']
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
    if (this.transferForm.invalid || !this.accountInfo) return;

    this.loading = true;
    this.errorMessage = '';

    this.userService.getAccountById(this.transferForm.value.toAccountId).subscribe({
      next: (recipient) => {
        this.loading = false;
        this.recipientInfo = recipient;
        this.confirming = true;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Could not find recipient account. Please check the account ID.';
      }
    });
  }

  confirmTransfer(): void {
    if (!this.accountInfo || !this.recipientInfo) return;

    this.loading = true;
    this.confirming = false;
    this.errorMessage = '';

    this.userService.transfer({
      toAccountId: this.recipientInfo.id,
      amount: this.transferForm.value.amount
    }).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.status === 'SUCCESS') {
          this.successMessage = `Transfer of ₹${response.amount} completed successfully! Transaction ID: ${response.transactionId}`;
          this.transferForm.reset();
          this.recipientInfo = null;
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

  cancelReview(): void {
    this.confirming = false;
    this.recipientInfo = null;
  }
}
