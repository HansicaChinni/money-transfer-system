import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';

import { AccountService } from '../../services/account-service.service';
import { AuthService } from '../../services/auth-service.service';
import { TransactionLogResponse } from '../../models/models';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule
  ],
  templateUrl: './history-component.component.html',
  styleUrls: ['./history-component.component.css']
})
export class HistoryComponent implements OnInit {
  transactions: TransactionLogResponse[] = [];
  displayedColumns: string[] = ['date', 'type', 'account', 'amount', 'status'];
  loading = true;
  errorMessage = '';
  currentAccountId: number | null = null;

  constructor(
    private accountService: AccountService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentAccountId = this.authService.getCurrentAccountId();
    this.loadTransactions();
  }

  loadTransactions(): void {
    this.accountService.getTransactions().subscribe({
      next: (transactions) => {
        this.transactions = transactions;
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to load transaction history';
        this.loading = false;
      }
    });
  }

  getTransactionType(tx: TransactionLogResponse): string {
    return tx.fromAccountId === this.currentAccountId ? 'DEBIT' : 'CREDIT';
  }

  getOtherAccountId(tx: TransactionLogResponse): number {
    return tx.fromAccountId === this.currentAccountId
      ? tx.toAccountId
      : tx.fromAccountId;
  }

  getTransactionIcon(tx: TransactionLogResponse): string {
    return this.getTransactionType(tx) === 'DEBIT'
      ? 'arrow_upward'
      : 'arrow_downward';
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }
}
