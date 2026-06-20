import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { RewardService } from '../../../core/services/reward.service';
import { UserService } from '../../../core/services/user.service';
import { RewardSummaryResponse, RewardTransactionResponse, TransactionDetailResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './rewards.component.html',
  styleUrl: './rewards.component.scss'
})
export class RewardsComponent implements OnInit {
  summary: RewardSummaryResponse | null = null;
  rewardTransactions: RewardTransactionResponse[] = [];
  loading = true;
  errorMessage = '';

  showTxModal = false;
  txDetail: TransactionDetailResponse | null = null;
  txDetailLoading = false;

  constructor(
    private rewardService: RewardService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.rewardService.getRewardSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
      },
      error: () => {
        this.errorMessage = 'Failed to load reward summary';
      }
    });

    this.rewardService.getRewardTransactions().subscribe({
      next: (transactions) => {
        this.rewardTransactions = transactions;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load reward transactions';
        this.loading = false;
      }
    });
  }

  openTxDetail(transactionId: number): void {
    this.txDetailLoading = true;
    this.showTxModal = true;
    this.txDetail = null;

    this.userService.getTransactionDetail(transactionId).subscribe({
      next: (detail) => {
        this.txDetail = detail;
        this.txDetailLoading = false;
      },
      error: () => {
        this.txDetail = null;
        this.txDetailLoading = false;
      }
    });
  }

  closeTxModal(): void {
    this.showTxModal = false;
    this.txDetail = null;
  }

  getTypeBadge(type: string): string {
    return type === 'EARNED' ? 'badge-earned' : 'badge-redeemed';
  }

  getStatusBadge(status: string): string {
    return status === 'SUCCESS' ? 'badge-tx-success' : 'badge-tx-failed';
  }
}
