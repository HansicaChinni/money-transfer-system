import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { AdminService } from '../../../core/services/admin.service';
import { TransactionLogResponse } from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-transactions',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.scss'
})
export class AdminTransactionsComponent implements OnInit {
  transactions: TransactionLogResponse[] = [];
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
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = 'Failed to load transactions';
        this.loading = false;
      }
    });
  }

  getStatusBadgeClass(status: string): string {
    return status === 'SUCCESS' ? 'badge-tx-success' : 'badge-tx-failed';
  }
}
