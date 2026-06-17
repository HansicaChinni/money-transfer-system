import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { AdminService } from '../../../core/services/admin.service';
import { RewardLogResponse, TransactionLogResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-transactions',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.scss'
})
export class AdminTransactionsComponent implements OnInit {
  transactions: TransactionLogResponse[] = [];
  rewards: RewardLogResponse[] = [];
  loading = true;
  errorMessage = '';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadTransactions();
  }

  loadTransactions(): void {
    this.adminService.getAllTransactions().subscribe({
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
    this.adminService.getAllRewards().subscribe({
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
}
