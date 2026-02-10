import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AccountService } from '../../services/account-service.service';
import { AuthService } from '../../services/auth-service.service';
import { AccountResponse } from '../../models/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './dashboard-component.component.html',
  styleUrls: ['./dashboard-component.component.css']
})
export class DashboardComponent implements OnInit {
  account: AccountResponse | null = null;
  loading = true;
  errorMessage = '';

  constructor(
    private accountService: AccountService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAccountData();
  }

  loadAccountData(): void {
    const accountId = this.authService.getCurrentAccountId();

    if (!accountId) {
      this.errorMessage = 'No account associated with this user';
      this.loading = false;
      return;
    }

    this.accountService.getAccount(accountId).subscribe({
      next: (account) => {
        this.account = account;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load account data';
        this.loading = false;
      }
    });
  }

  navigateToTransfer(): void {
    this.router.navigate(['/transfer']);
  }

  navigateToHistory(): void {
    this.router.navigate(['/history']);
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }
}
