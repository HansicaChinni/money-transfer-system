import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { AdminService } from '../../../core/services/admin.service';
import { AdminAccountView, TransactionLogResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit {
  accounts: AdminAccountView[] = [];
  recentTransactions: TransactionLogResponse[] = [];
  loading = true;
  errorMessage = '';

  totalAccounts = 0;
  activeAccounts = 0;
  totalBalance = 0;
  recentTransactionCount = 0;
  totalPointsEarned = 0;
  totalPointsRedeemed = 0;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading = true;

    this.adminService.getAllAccounts().subscribe({
      next: (accounts) => {
        this.accounts = accounts;
        this.totalAccounts = accounts.length;
        this.activeAccounts = accounts.filter(a => a.status === 'ACTIVE').length;
        this.totalBalance = accounts.reduce((sum, acc) => sum + Number(acc.balance), 0);
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load accounts';
        this.loading = false;
      }
    });

    this.adminService.getAllTransactions().subscribe({
      next: (transactions) => {
        this.recentTransactions = transactions.slice(0, 5);
        this.recentTransactionCount = transactions.filter(t => t.status === 'SUCCESS').length;
      },
      error: () => {
        console.error('Failed to load transactions');
      }
    });

    this.adminService.getRewardDashboard().subscribe({
      next: (data) => {
        this.totalPointsEarned = data.totalPointsEarned;
        this.totalPointsRedeemed = data.totalPointsRedeemed;
      },
      error: () => {
        console.error('Failed to load reward dashboard');
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
