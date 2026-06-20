import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { UserService } from '../../../core/services/user.service';
import { AccountResponse, TransactionLogResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  accountInfo: AccountResponse | null = null;
  recentTransactions: TransactionLogResponse[] = [];
  loading = true;
  errorMessage = '';

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading = true;

    this.userService.getBalance().subscribe({
      next: (account) => {
        this.accountInfo = account;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load account information';
        this.loading = false;
      }
    });

    this.userService.getTransactions().subscribe({
      next: (transactions) => {
        this.recentTransactions = transactions.slice(0, 5);
      },
      error: () => {
        console.error('Failed to load transactions');
      }
    });
  }

  getStatusBadgeClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'ACTIVE':
        return 'badge-active';
      case 'LOCKED':
        return 'badge-locked';
      case 'CLOSED':
        return 'badge-closed';
      default:
        return 'badge-primary';
    }
  }

  getTransactionStatusBadge(status: string): string {
    return status === 'SUCCESS' ? 'badge-tx-success' : 'badge-tx-failed';
  }
}
