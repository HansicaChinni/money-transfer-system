import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { TransactionLogResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.scss'
})
export class TransactionsComponent implements OnInit {
  transactions: TransactionLogResponse[] = [];
  accountId: number | null = null;
  loading = true;
  errorMessage = '';

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
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load transactions';
        this.loading = false;
      }
    });
  }

  isOutgoing(tx: TransactionLogResponse): boolean {
    return tx.fromAccountId === this.accountId;
  }

  isIncoming(tx: TransactionLogResponse): boolean {
    return tx.toAccountId === this.accountId;
  }

  getCounterpartyName(tx: TransactionLogResponse): string {
    return this.isOutgoing(tx) ? tx.toHolderName : tx.fromHolderName;
  }

  getStatusBadgeClass(status: string): string {
    return status === 'SUCCESS' ? 'badge-tx-success' : 'badge-tx-failed';
  }
}
