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
  allTransactions: TransactionLogResponse[] = [];
  recentTransactions: TransactionLogResponse[] = [];
  loading = true;
  errorMessage = '';

  totalAccounts = 0;
  activeAccounts = 0;
  lockedAccounts = 0;
  closedAccounts = 0;
  totalBalance = 0;
  successCount = 0;
  failedCount = 0;
  topAccounts: AdminAccountView[] = [];

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
        this.lockedAccounts = accounts.filter(a => a.status === 'LOCKED').length;
        this.closedAccounts = accounts.filter(a => a.status === 'CLOSED').length;
        this.totalBalance = accounts.reduce((sum, acc) => sum + Number(acc.balance), 0);
        this.topAccounts = [...accounts].sort((a, b) => Number(b.balance) - Number(a.balance)).slice(0, 5);
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = 'Failed to load accounts';
        this.loading = false;
      }
    });

    this.adminService.getAllTransactions().subscribe({
      next: (transactions) => {
        this.allTransactions = transactions;
        this.recentTransactions = transactions.slice(0, 5);
        this.successCount = transactions.filter(t => t.status === 'SUCCESS').length;
        this.failedCount = transactions.filter(t => t.status === 'FAILED').length;
      },
      error: (error) => {
        console.error('Failed to load transactions', error);
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

  getTransactionStatusBadge(status: string): string {
    return status === 'SUCCESS' ? 'badge-success' : 'badge-danger';
  }

  transactionDonutStyle(): string {
    const total = this.successCount + this.failedCount;
    if (total === 0) return '';
    const sPct = (this.successCount / total) * 100;
    return `conic-gradient(#10B981 0% ${sPct}%, #DC2626 ${sPct}% 100%)`;
  }

  donutStyle(active: number, locked: number, closed: number): string {
    const total = active + locked + closed;
    if (total === 0) return '';
    const aPct = (active / total) * 100;
    const lPct = (locked / total) * 100;
    const cPct = (closed / total) * 100;
    const aEnd = aPct;
    const lEnd = aPct + lPct;
    return `conic-gradient(
      var(--wine-primary) 0% ${aEnd}%,
      var(--accent-orange-dark) ${aEnd}% ${lEnd}%,
      var(--text-light) ${lEnd}% ${lEnd + cPct}%
    )`;
  }

  maxAccountBalance(): number {
    if (this.topAccounts.length === 0) return 1;
    return Math.max(...this.topAccounts.map(a => Number(a.balance)));
  }

  barPercent(amount: any): number {
    return (Number(amount) / this.maxAccountBalance()) * 100;
  }
}
