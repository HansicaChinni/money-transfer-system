import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { AdminService } from '../../../core/services/admin.service';
import { AdminAccountView } from '../../../core/models/api.models';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent, FormsModule],
  templateUrl: './accounts.component.html',
  styleUrl: './accounts.component.scss'
})
export class AccountsComponent implements OnInit {
  accounts: AdminAccountView[] = [];
  filteredAccounts: AdminAccountView[] = [];
  loading = true;
  errorMessage = '';
  searchAccountId = '';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loading = true;
    this.adminService.getAllAccounts().subscribe({
      next: (accounts) => {
        this.accounts = accounts;
        this.filteredAccounts = accounts;
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = 'Failed to load accounts';
        this.loading = false;
      }
    });
  }

  filterAccounts(): void {
    if (!this.searchAccountId.trim()) {
      this.filteredAccounts = this.accounts;
      return;
    }

    const searchTerm = this.searchAccountId.trim().toLowerCase();
    this.filteredAccounts = this.accounts.filter(account => 
      account.id.toString().includes(searchTerm)
    );
  }

  getStatusBadgeClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'ACTIVE':
        return 'badge-success';
      case 'LOCKED':
        return 'badge-warning';
      case 'CLOSED':
        return 'badge-danger';
      default:
        return 'badge-primary';
    }
  }
}