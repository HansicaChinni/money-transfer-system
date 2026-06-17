import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { RewardLogResponse, TransactionLogResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.scss'
})
export class TransactionsComponent implements OnInit {
  transactions: TransactionLogResponse[] = [];
  rewards: RewardLogResponse[] = [];
  loading = true;
  errorMessage = '';
  accountId: number | null = null;

  constructor(
    private userService: UserService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.accountId = this.authService.getAccountId();
    this.loadTransactions();
  }

  loadTransactions(): void {
    this.loading = true;
    this.userService.getTransactions().subscribe({
      next: (transactions) => {
        this.transactions = transactions;
        this.loadRewards();
      },
      error: (error) => {
        this.errorMessage = 'Failed to load transactions';
        this.loading = false;
      }
    });
  }

  loadRewards(): void {
    this.userService.getRewards().subscribe({
      next: (rewards) => {
        this.rewards = rewards;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load rewards';
        this.loading = false;
      }
    });
  }

  getTotalRewardPoints(): number {
    return this.rewards.reduce((total, reward) => total + reward.points, 0);
  }

  getStatusBadgeClass(status: string): string {
    return status === 'SUCCESS' ? 'badge-success' : 'badge-danger';
  }

  isOutgoing(tx: TransactionLogResponse): boolean {
    return tx.fromAccountId === this.accountId;
  }

  isIncoming(tx: TransactionLogResponse): boolean {
    return tx.toAccountId === this.accountId;
  }
}
