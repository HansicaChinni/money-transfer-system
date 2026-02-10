import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AdminService } from '../../services/admin-service.service';
import { AdminAccountView, TransactionLogResponse } from '../../models/models';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatTabsModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './admin-component.component.html',
  styleUrls: ['./admin-component.component.css']
})
export class AdminComponent implements OnInit {
  accounts: AdminAccountView[] = [];
  transactions: TransactionLogResponse[] = [];

  accountColumns: string[] = ['id', 'balance', 'status', 'lastUpdated'];
  transactionColumns: string[] = ['id', 'from', 'to', 'amount', 'status', 'date'];

  loadingAccounts = true;
  loadingTransactions = true;

  errorMessageAccounts = '';
  errorMessageTransactions = '';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadAccounts();
    this.loadTransactions();
  }

  loadAccounts(): void {
    this.adminService.getAllAccounts().subscribe({
      next: (accounts) => {
        this.accounts = accounts;
        this.loadingAccounts = false;
      },
      error: () => {
        this.errorMessageAccounts = 'Failed to load accounts';
        this.loadingAccounts = false;
      }
    });
  }

  loadTransactions(): void {
    this.adminService.getAllTransactions().subscribe({
      next: (transactions) => {
        this.transactions = transactions;
        this.loadingTransactions = false;
      },
      error: () => {
        this.errorMessageTransactions = 'Failed to load transactions';
        this.loadingTransactions = false;
      }
    });
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  getTotalBalance(): number {
    return this.accounts.reduce((sum, acc) => sum + acc.balance, 0);
  }

  getSuccessfulTransactions(): number {
    return this.transactions.filter(t => t.status === 'SUCCESS').length;
  }

  getFailedTransactions(): number {
    return this.transactions.filter(t => t.status === 'FAILED').length;
  }

  getTotalTransactionVolume(): number {
    return this.transactions
      .filter(t => t.status === 'SUCCESS')
      .reduce((sum, t) => sum + t.amount, 0);
  }
}
