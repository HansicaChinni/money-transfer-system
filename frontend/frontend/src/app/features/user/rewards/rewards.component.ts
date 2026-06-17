import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { UserService } from '../../../core/services/user.service';
import {
  RewardSummaryResponse,
  RewardTransactionResponse,
  RewardItemResponse,
  RedemptionResponse
} from '../../../core/models/api.models';

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './rewards.component.html',
  styleUrl: './rewards.component.scss'
})
export class RewardsComponent implements OnInit {
  summary: RewardSummaryResponse | null = null;
  history: RewardTransactionResponse[] = [];
  store: RewardItemResponse[] = [];
  redemptions: RedemptionResponse[] = [];
  loading = true;
  errorMessage = '';
  redeemSuccess = '';
  activeTab: 'store' | 'history' | 'coupons' = 'store';

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.userService.getRewardSummary().subscribe({
      next: (s) => { this.summary = s; },
      error: () => {}
    });
    this.userService.getRewardHistory().subscribe({
      next: (h) => { this.history = h; },
      error: () => {}
    });
    this.userService.getRewardStore().subscribe({
      next: (s) => { this.store = s; },
      error: () => {}
    });
    this.userService.getRedemptions().subscribe({
      next: (r) => { this.redemptions = r; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  redeem(item: RewardItemResponse): void {
    this.redeemSuccess = '';
    this.errorMessage = '';
    this.userService.redeem({ rewardItemId: item.id }).subscribe({
      next: () => {
        this.redeemSuccess = `Successfully redeemed ${item.name}!`;
        this.loadAll();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Redemption failed';
      }
    });
  }

  setTab(tab: 'store' | 'history' | 'coupons'): void {
    this.activeTab = tab;
  }
}
