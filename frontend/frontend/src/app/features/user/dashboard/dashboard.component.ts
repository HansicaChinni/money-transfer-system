import { Component, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { AccountResponse, TransactionLogResponse } from '../../../core/models/api.models';
import Chart from 'chart.js/auto';

interface QuickContact {
  accountNumber: string;
  holderName: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, AfterViewInit {
  accountInfo: AccountResponse | null = null;
  allTransactions: TransactionLogResponse[] = [];
  recentTransactions: TransactionLogResponse[] = [];
  quickContacts: QuickContact[] = [];
  loading = true;
  errorMessage = '';
  private _chartsInitialized = false;
  private _spendingChart: Chart | null = null;
  private _incomeChart: Chart | null = null;

  private avatarColors = [
    'var(--leaf-primary)',
    'var(--leaf-dark)',
    'var(--leaf-medium)',
    'var(--leaf-light)',
    'var(--lime-accent)',
    'var(--wine-accent-light)'
  ];

  constructor(
    private userService: UserService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngAfterViewInit(): void {
    if (!this.loading && this.allTransactions.length > 0) {
      this.buildCharts();
    }
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
        this.allTransactions = transactions;
        this.recentTransactions = transactions.slice(0, 5);
        this.buildQuickContacts(transactions);
        if (!this._chartsInitialized && transactions.length > 0) {
          this.buildCharts();
        }
      },
      error: () => {
        console.error('Failed to load transactions');
      }
    });
  }

  private buildCharts(): void {
    this._chartsInitialized = true;
    setTimeout(() => this._renderCharts(), 100);
  }

  private _renderCharts(): void {
    const accountId = this.accountInfo?.id;
    if (!accountId) return;

    const spendingCanvas = document.getElementById('spendingChart') as HTMLCanvasElement | null;
    const incomeCanvas = document.getElementById('incomeChart') as HTMLCanvasElement | null;

    if (spendingCanvas) {
      if (this._spendingChart) this._spendingChart.destroy();
      const { labels, data } = this._computeDailySpending(accountId, 7);
      this._spendingChart = new Chart(spendingCanvas, {
        type: 'bar',
        data: {
          labels,
          datasets: [{
            label: 'Spent',
            data,
            backgroundColor: 'rgba(143, 193, 181, 0.65)',
            borderColor: '#8fc1b5',
            borderWidth: 1,
            borderRadius: 4
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: {
            y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' } },
            x: { grid: { display: false } }
          }
        }
      });
    }

    if (incomeCanvas) {
      if (this._incomeChart) this._incomeChart.destroy();
      const { labels, incoming, outgoing } = this._computeMonthlySummary(accountId, 30);
      this._incomeChart = new Chart(incomeCanvas, {
        type: 'bar',
        data: {
          labels,
          datasets: [
            { label: 'Received', data: incoming, backgroundColor: 'rgba(143, 193, 181, 0.8)', borderRadius: 4 },
            { label: 'Sent', data: outgoing, backgroundColor: 'rgba(222, 236, 199, 0.8)', borderRadius: 4 }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { position: 'top', labels: { boxWidth: 12, padding: 8 } } },
          scales: {
            y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' } },
            x: { grid: { display: false } }
          }
        }
      });
    }
  }

  private _computeDailySpending(accountId: number, days: number): { labels: string[]; data: number[] } {
    const labels: string[] = [];
    const data: number[] = [];
    const now = new Date();
    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      labels.push(d.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric' }));
      const dayStart = new Date(d.getFullYear(), d.getMonth(), d.getDate());
      const dayEnd = new Date(dayStart.getTime() + 86400000);
      let total = 0;
      for (const tx of this.allTransactions) {
        if (tx.fromAccountId === accountId && tx.createdOn) {
          const txDate = new Date(tx.createdOn);
          if (txDate >= dayStart && txDate < dayEnd) {
            total += tx.amount;
          }
        }
      }
      data.push(total);
    }
    return { labels, data };
  }

  private _computeMonthlySummary(accountId: number, days: number): { labels: string[]; incoming: number[]; outgoing: number[] } {
    const labels: string[] = [];
    const incoming: number[] = [];
    const outgoing: number[] = [];
    const now = new Date();
    const weeks = Math.ceil(days / 7);
    for (let w = weeks - 1; w >= 0; w--) {
      const start = new Date(now);
      start.setDate(start.getDate() - (w * 7) - 6);
      const end = new Date(now);
      end.setDate(end.getDate() - (w * 7));
      if (w === weeks - 1) start.setDate(start.getDate() + 7);
      labels.push(`W${weeks - w}`);
      let inc = 0;
      let out = 0;
      for (const tx of this.allTransactions) {
        if (!tx.createdOn) continue;
        const txDate = new Date(tx.createdOn);
        if (txDate >= start && txDate <= end) {
          if (tx.toAccountId === accountId) inc += tx.amount;
          if (tx.fromAccountId === accountId) out += tx.amount;
        }
      }
      incoming.push(inc);
      outgoing.push(out);
    }
    return { labels, incoming, outgoing };
  }

  private buildQuickContacts(transactions: TransactionLogResponse[]): void {
    const seen = new Set<string>();
    this.quickContacts = [];

    for (const tx of transactions) {
      if (!this.accountInfo) break;

      const isOutgoing = tx.fromAccountId === this.accountInfo.id;
      const contactNumber = isOutgoing ? tx.toAccountNumber : tx.fromAccountNumber;
      const contactName = isOutgoing ? tx.toHolderName : tx.fromHolderName;

      if (contactNumber && !seen.has(contactNumber)) {
        seen.add(contactNumber);
        this.quickContacts.push({ accountNumber: contactNumber, holderName: contactName });

        if (this.quickContacts.length === 4) break;
      }
    }
  }

  getInitials(name: string): string {
    if (!name) return '?';
    return name
      .split(' ')
      .filter(part => part.length > 0)
      .map(part => part[0].toUpperCase())
      .slice(0, 2)
      .join('');
  }

  getAvatarColor(accountNumber: string): string {
    let hash = 0;
    for (let i = 0; i < accountNumber.length; i++) {
      hash = accountNumber.charCodeAt(i) + ((hash << 5) - hash);
    }
    const index = Math.abs(hash) % this.avatarColors.length;
    return this.avatarColors[index];
  }

  isOutgoing(tx: TransactionLogResponse): boolean {
    return tx.fromAccountId === this.accountInfo?.id;
  }

  isIncoming(tx: TransactionLogResponse): boolean {
    return tx.toAccountId === this.accountInfo?.id;
  }

  getCounterpartyName(tx: TransactionLogResponse): string {
    return this.isOutgoing(tx) ? tx.toHolderName : tx.fromHolderName;
  }

  getCounterpartyAccount(tx: TransactionLogResponse): string {
    return this.isOutgoing(tx) ? tx.toAccountNumber : tx.fromAccountNumber;
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
