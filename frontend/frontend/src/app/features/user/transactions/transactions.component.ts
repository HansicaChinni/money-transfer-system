import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { AccountResponse, TransactionLogResponse } from '../../../core/models/api.models';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

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
  accountInfo: AccountResponse | null = null;
  loading = true;
  errorMessage = '';

  constructor(
    private userService: UserService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.accountId = this.authService.getAccountId();
    this.loadAccountInfo();
    this.loadTransactions();
  }

  loadAccountInfo(): void {
    this.userService.getBalance().subscribe({
      next: (account) => {
        this.accountInfo = account;
      },
      error: () => {}
    });
  }

  exportPdf(): void {
    const doc = new jsPDF({ orientation: 'landscape' });
    const now = new Date();
    const dateStr = now.toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });
    const timeStr = now.toLocaleTimeString('en-IN');

    doc.setFontSize(18);
    doc.text('Transaction History', 14, 20);

    doc.setFontSize(10);
    let y = 28;
    if (this.accountInfo) {
      doc.text(`Account Holder: ${this.accountInfo.holderName}`, 14, y);
      y += 5;
      doc.text(`Account Number: ${this.accountInfo.accountNumber}`, 14, y);
      y += 7;
    } else {
      y += 4;
    }
    doc.text(`Generated: ${dateStr} ${timeStr}`, 14, y);

    const rows = this.transactions.map(tx => {
      const isOutgoing = tx.fromAccountId === this.accountId;
      const counterparty = isOutgoing ? tx.toHolderName : tx.fromHolderName;
      const type = isOutgoing ? 'Sent' : 'Received';
      const prefix = isOutgoing ? '-' : '+';
      const amt = tx.amount != null ? tx.amount : 0;
      const amount = prefix + ' Rs.' + amt.toFixed(2);
      const earned = tx.rewardPointsEarned ? '+' + tx.rewardPointsEarned : '';
      const redeemed = tx.rewardPointsUsed ? '-' + tx.rewardPointsUsed : '';
      const points = earned && redeemed ? earned + ' / ' + redeemed : earned || redeemed || '-';
      const date = tx.createdOn
        ? new Date(tx.createdOn).toLocaleDateString('en-IN', {
            year: 'numeric', month: 'short', day: 'numeric',
            hour: '2-digit', minute: '2-digit'
          })
        : '';
      return ['#' + tx.id, date, type, counterparty, amount, points, tx.status];
    });

    autoTable(doc, {
      startY: y + 4,
      head: [['ID', 'Date & Time', 'Type', 'Counterparty', 'Amount', 'Points', 'Status']],
      body: rows,
      theme: 'striped',
      headStyles: {
        fillColor: [143, 193, 181],
        textColor: [255, 255, 255],
        fontStyle: 'bold'
      },
      alternateRowStyles: {
        fillColor: [245, 250, 248]
      },
      styles: {
        fontSize: 8,
        cellPadding: 3
      },
      columnStyles: {
        0: { cellWidth: 20 },
        4: { halign: 'right' }
      }
    });

    doc.save('transaction-history.pdf');
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
