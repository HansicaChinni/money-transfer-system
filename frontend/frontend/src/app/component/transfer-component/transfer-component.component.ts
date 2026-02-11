import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule } from '@angular/material/dialog';

import { TransferService } from '../../services/transfer-service.service';
import { AuthService } from '../../services/auth-service.service';
import { AccountService } from '../../services/account-service.service';
import { MeTransferRequest, AccountResponse } from '../../models/models';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule
  ],
  templateUrl: './transfer-component.component.html',
  styleUrls: ['./transfer-component.component.css']
})
export class TransferComponent implements OnInit {
  transferForm: FormGroup;
  loading = false;
  successMessage = '';
  errorMessage = '';
  currentAccount: AccountResponse | null = null;
  loadingAccount = true;

  constructor(
    private fb: FormBuilder,
    private transferService: TransferService,
    private authService: AuthService,
    private accountService: AccountService
  ) {
    this.transferForm = this.fb.group({
      toAccountId: ['', [Validators.required, Validators.min(1)]],
      amount: ['', [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit(): void {
    this.loadCurrentAccount();
  }

  loadCurrentAccount(): void {
    this.accountService.getAccount().subscribe({
      next: (account) => {
        this.currentAccount = account;
        this.loadingAccount = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to load account data';
        this.loadingAccount = false;
      }
    });
  }

  onSubmit(): void {
    if (this.transferForm.invalid || !this.currentAccount) {
      return;
    }

    const { toAccountId, amount } = this.transferForm.value;
    const currentAccountId = this.authService.getCurrentAccountId();

    if (toAccountId === currentAccountId) {
      this.errorMessage = 'Cannot transfer to your own account';
      return;
    }

    if (amount > this.currentAccount.balance) {
      this.errorMessage = 'Insufficient balance';
      return;
    }

    const request: MeTransferRequest = { toAccountId, amount };

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.transferService.transferForCurrentUser(request).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = `Transfer successful! Transaction ID: ${response.transactionId}`;
        this.transferForm.reset();
        this.loadCurrentAccount();

        setTimeout(() => (this.successMessage = ''), 5000);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Transfer failed';
      }
    });
  }

  resetForm(): void {
    this.transferForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
  }

  getStatusClass(status: string): string {
    switch (status?.toLowerCase()) {
      case 'active':
        return 'status-active';
      case 'inactive':
        return 'status-inactive';
      case 'suspended':
        return 'status-suspended';
      default:
        return 'status-unknown';
    }
  }
}
