import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { AdminService } from '../../../core/services/admin.service';
import { RewardTransactionResponse, TransactionDetailResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-rewards',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent],
  templateUrl: './rewards.component.html',
  styleUrl: './rewards.component.scss'
})
export class AdminRewardsComponent implements OnInit {
  rewardTransactions: RewardTransactionResponse[] = [];
  loading = true;
  errorMessage = '';

  showTxModal = false;
  txDetail: TransactionDetailResponse | null = null;
  txDetailLoading = false;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadRewardTransactions();
  }

  loadRewardTransactions(): void {
    this.loading = true;
    this.adminService.getAllRewardTransactions().subscribe({
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

    this.adminService.getTransactionDetail(transactionId).subscribe({
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
