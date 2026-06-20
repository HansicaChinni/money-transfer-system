import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { UserService } from '../../../core/services/user.service';
import { AccountResponse, RewardSummaryResponse } from '../../../core/models/api.models';
import { RewardService } from '../../../core/services/reward.service';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './transfer.component.html',
  styleUrl: './transfer.component.scss'
})
export class TransferComponent implements OnInit {
  transferForm: FormGroup;
  loading = false;
  successMessage = '';
  errorMessage = '';
  accountInfo: AccountResponse | null = null;
  rewardSummary: RewardSummaryResponse | null = null;
  rewardEstimate: { used: number; cashDebit: number } | null = null;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private rewardService: RewardService,
    private router: Router
  ) {
    this.transferForm = this.fb.group({
      toAccountNumber: ['', [Validators.required, Validators.pattern(/^ACC-\d{4}-\d{6}$/)]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      useRewardPoints: [false]
    });
  }

  ngOnInit(): void {
    this.loadAccountInfo();
    this.loadRewardSummary();
    this.transferForm.get('amount')?.valueChanges.subscribe(() => this.updateRewardEstimate());
    this.transferForm.get('useRewardPoints')?.valueChanges.subscribe(() => this.updateRewardEstimate());
  }

  loadAccountInfo(): void {
    this.userService.getBalance().subscribe({
      next: (account) => {
        this.accountInfo = account;
      },
      error: (error) => {
        console.error('Failed to load account info', error);
      }
    });
  }

  loadRewardSummary(): void {
    this.rewardService.getRewardSummary().subscribe({
      next: (summary) => {
        this.rewardSummary = summary;
      },
      error: () => {
        this.rewardSummary = null;
      }
    });
  }

  updateRewardEstimate(): void {
    this.rewardEstimate = null;
    const useRewards = this.transferForm.get('useRewardPoints')?.value;
    if (!useRewards || !this.rewardSummary) return;

    const amountVal = parseFloat(this.transferForm.get('amount')?.value);
    if (!amountVal || amountVal <= 0) return;

    const available = this.rewardSummary.currentPoints;
    const used = Math.min(available, Math.floor(amountVal));
    const cashDebit = amountVal - used;
    this.rewardEstimate = { used, cashDebit };
  }

  onSubmit(): void {
    if (this.transferForm.invalid) {
      return;
    }

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.userService.transfer(this.transferForm.value).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.status === 'SUCCESS') {
          let msg = `Transfer of ₹${response.amount} completed successfully! Transaction ID: ${response.transactionId}`;
          if (response.rewardPointsUsed) {
            msg += ` | Reward points used: ${response.rewardPointsUsed}`;
          }
          if (response.rewardPointsEarned) {
            msg += ` | Reward points earned: ${response.rewardPointsEarned}`;
          }
          this.successMessage = msg;
          this.transferForm.reset({ useRewardPoints: false });
          this.rewardEstimate = null;
          this.loadAccountInfo();
          this.loadRewardSummary();
          setTimeout(() => {
            this.router.navigate(['/user/transactions']);
          }, 2000);
        } else {
          this.errorMessage = response.message || 'Transfer failed';
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Transfer failed. Please try again.';
      }
    });
  }
}
