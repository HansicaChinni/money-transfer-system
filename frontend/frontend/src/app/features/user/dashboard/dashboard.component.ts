import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { UserService } from '../../../core/services/user.service';
import { AccountResponse, TransactionLogResponse, RewardSummaryResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  accountInfo: AccountResponse | null = null;
  allTransactions: TransactionLogResponse[] = [];
  recentTransactions: TransactionLogResponse[] = [];
  rewardSummary: RewardSummaryResponse | null = null;
  loading = true;
  errorMessage = '';

  sentCount = 0;
  receivedCount = 0;
  sentTotal = 0;
  receivedTotal = 0;

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
      error: (error) => {
        this.errorMessage = 'Failed to load account information';
        this.loading = false;
      }
    });

    this.userService.getTransactions().subscribe({
      next: (transactions) => {
        this.allTransactions = transactions;
        this.recentTransactions = transactions.slice(0, 5);
        this.computeAnalytics();
      },
      error: (error) => {
        console.error('Failed to load transactions', error);
      }
    });

    this.userService.getRewardSummary().subscribe({
      next: (summary) => {
        this.rewardSummary = summary;
      },
      error: () => {}
    });
  }

  computeAnalytics(): void {
    if (!this.accountInfo) return;
    const accId = this.accountInfo.id;
    this.sentCount = this.allTransactions.filter(t => t.fromAccountId === accId).length;
    this.receivedCount = this.allTransactions.filter(t => t.toAccountId === accId).length;
    this.sentTotal = this.allTransactions
      .filter(t => t.fromAccountId === accId && t.status === 'SUCCESS')
      .reduce((s, t) => s + Number(t.amount), 0);
    this.receivedTotal = this.allTransactions
      .filter(t => t.toAccountId === accId && t.status === 'SUCCESS')
      .reduce((s, t) => s + Number(t.amount), 0);
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

  maxBarValue(): number {
    if (this.recentTransactions.length === 0) return 1;
    return Math.max(...this.recentTransactions.map(t => Number(t.amount)));
  }

  barPercent(amount: any): number {
    return (Number(amount) / this.maxBarValue()) * 100;
  }

  donutStyle(sent: number, received: number): string {
    const total = sent + received;
    if (total === 0) return '';
    const sentPct = (sent / total) * 100;
    return `conic-gradient(var(--wine-primary) 0% ${sentPct}%, var(--wine-light) ${sentPct}% 100%)`;
  }
}
