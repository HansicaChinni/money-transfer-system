import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { UserService } from '../../../core/services/user.service';
import { AccountResponse, TransactionLogResponse } from '../../../core/models/api.models';

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
export class DashboardComponent implements OnInit {
  accountInfo: AccountResponse | null = null;
  recentTransactions: TransactionLogResponse[] = [];
  quickContacts: QuickContact[] = [];
  loading = true;
  errorMessage = '';

  private avatarColors = [
    'var(--wine-primary)',
    'var(--wine-dark)',
    'var(--wine-medium)',
    'var(--wine-light)',
    'var(--wine-accent)',
    'var(--wine-accent-light)'
  ];

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
        this.buildQuickContacts(transactions);
      },
      error: () => {
        console.error('Failed to load transactions');
      }
    });
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
