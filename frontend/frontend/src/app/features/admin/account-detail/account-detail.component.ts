import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { AdminService } from '../../../core/services/admin.service';
import { AdminAccountDetailResponse, TransactionLogResponse, AccountStatus } from '../../../core/models/api.models';

@Component({
  selector: 'app-account-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, NavbarComponent],
  templateUrl: './account-detail.component.html',
  styleUrl: './account-detail.component.scss'
})
export class AccountDetailComponent implements OnInit {
  accountId!: number;
  account: AdminAccountDetailResponse | null = null;
  transactions: TransactionLogResponse[] = [];
  loading = true;
  errorMessage = '';
  successMessage = '';
  accountStatuses = Object.values(AccountStatus);
  AccountStatus = AccountStatus;
  editingLimit = false;
  newDailyLimit = 0;

  constructor(
    private route: ActivatedRoute,
    private adminService: AdminService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.accountId = +params['id'];
      this.loadAccountDetails();
      this.loadTransactions();
    });
  }

  loadAccountDetails(): void {
    this.adminService.getAccountDetails(this.accountId).subscribe({
      next: (account) => {
        this.account = account;
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = 'Failed to load account details';
        this.loading = false;
      }
    });
  }

  loadTransactions(): void {
    this.adminService.getAccountTransactions(this.accountId).subscribe({
      next: (transactions) => {
        this.transactions = transactions;
      },
      error: (error) => {
        console.error('Failed to load transactions', error);
      }
    });
  }

  updateStatus(status: AccountStatus): void {
    this.adminService.updateAccountStatus(this.accountId, status).subscribe({
      next: (account) => {
        this.account = account;
        this.successMessage = `Account status updated to ${status}`;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (error) => {
        this.errorMessage = 'Failed to update account status';
      }
    });
  }

  getStatusBadgeClass(status: string): string {
    const s = status?.toUpperCase();
    return s === 'ACTIVE' ? 'badge-success' : s === 'LOCKED' ? 'badge-warning' : 'badge-danger';
  }

  getTransactionStatusBadge(status: string): string {
    return status === 'SUCCESS' ? 'badge-success' : 'badge-danger';
  }

  startEditLimit(): void {
    this.newDailyLimit = this.account?.dailyTransferLimit ?? 50000;
    this.editingLimit = true;
  }

  cancelEditLimit(): void {
    this.editingLimit = false;
  }

  saveDailyLimit(): void {
    if (!this.account || this.newDailyLimit <= 0) return;
    this.adminService.updateDailyLimit(this.account.id, this.newDailyLimit).subscribe({
      next: (account) => {
        this.account = account;
        this.editingLimit = false;
        this.successMessage = `Daily transfer limit updated to ₹${this.newDailyLimit.toLocaleString()}`;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = 'Failed to update daily limit';
      }
    });
  }
}
