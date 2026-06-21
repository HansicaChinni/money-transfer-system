import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  RewardItemResponse,
  RedemptionResponse,
  CreateRewardItemRequest,
  FulfillRequest
} from '../../../core/models/api.models';

@Component({
  selector: 'app-admin-rewards',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './rewards.component.html',
  styleUrl: './rewards.component.scss'
})
export class AdminRewardsComponent implements OnInit {
  items: RewardItemResponse[] = [];
  redemptions: RedemptionResponse[] = [];
  loading = true;
  successMessage = '';
  errorMessage = '';

  showItemForm = false;
  editingItem: RewardItemResponse | null = null;
  itemForm: CreateRewardItemRequest = {
    name: '',
    description: '',
    brand: '',
    pointsRequired: 100,
    couponValue: 50,
    imageUrl: null
  };

  fulfillNotes = '';

  currentRatio = 100;
  showRatioModal = false;
  newRatio = 100;
  ratioConfirmText = '';

  constructor(
    private adminService: AdminService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAll();
    this.loadRatio();
  }

  loadAll(): void {
    this.loading = true;
    this.adminService.getRewardItems().subscribe({
      next: (items) => { this.items = items; },
      error: () => {}
    });
    this.adminService.getRedemptions().subscribe({
      next: (r) => { this.redemptions = r; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  loadRatio(): void {
    this.adminService.getRewardRatio().subscribe({
      next: (r) => { this.currentRatio = r.pointsPerUnit; },
      error: () => {}
    });
  }

  openRatioModal(): void {
    this.newRatio = this.currentRatio;
    this.ratioConfirmText = '';
    this.showRatioModal = true;
  }

  closeRatioModal(): void {
    this.showRatioModal = false;
  }

  confirmRatioChange(): void {
    if (this.ratioConfirmText !== 'confirm' || this.newRatio <= 0) return;
    if (!this.authService.isLoggedIn()) {
      this.errorMessage = 'Session expired. Please login again.';
      this.closeRatioModal();
      return;
    }
    this.adminService.updateRewardRatio(this.newRatio).subscribe({
      next: () => {
        this.currentRatio = this.newRatio;
        this.successMessage = `Reward ratio updated to 1 pt per ₹${this.newRatio}`;
        this.closeRatioModal();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update ratio';
      }
    });
  }

  openNewItem(): void {
    this.editingItem = null;
    this.itemForm = { name: '', description: '', brand: '', pointsRequired: 100, couponValue: 50, imageUrl: null };
    this.showItemForm = true;
  }

  openEditItem(item: RewardItemResponse): void {
    this.editingItem = item;
    this.itemForm = {
      name: item.name,
      description: item.description,
      brand: item.brand,
      pointsRequired: item.pointsRequired,
      couponValue: item.couponValue,
      imageUrl: item.imageUrl
    };
    this.showItemForm = true;
  }

  cancelForm(): void {
    this.showItemForm = false;
    this.editingItem = null;
  }

  saveItem(): void {
    if (this.editingItem) {
      this.adminService.updateRewardItem(this.editingItem.id, this.itemForm).subscribe({
        next: () => { this.successMessage = 'Item updated'; this.cancelForm(); this.loadAll(); },
        error: (err) => { this.errorMessage = err.error?.message || 'Update failed'; }
      });
    } else {
      this.adminService.createRewardItem(this.itemForm).subscribe({
        next: () => { this.successMessage = 'Item created'; this.cancelForm(); this.loadAll(); },
        error: (err) => { this.errorMessage = err.error?.message || 'Create failed'; }
      });
    }
  }

  deleteItem(id: number): void {
    if (!confirm('Delete this reward item?')) return;
    this.adminService.deleteRewardItem(id).subscribe({
      next: () => { this.successMessage = 'Item deleted'; this.loadAll(); },
      error: (err) => { this.errorMessage = err.error?.message || 'Delete failed'; }
    });
  }

  fulfillRedemption(id: number): void {
    const req: FulfillRequest = { notes: this.fulfillNotes };
    this.adminService.fulfillRedemption(id, req).subscribe({
      next: () => { this.successMessage = 'Redemption fulfilled'; this.fulfillNotes = ''; this.loadAll(); },
      error: (err) => { this.errorMessage = err.error?.message || 'Fulfill failed'; }
    });
  }

  cancelRedemption(id: number): void {
    if (!confirm('Cancel this redemption? Points will be refunded.')) return;
    this.adminService.cancelRedemption(id).subscribe({
      next: () => { this.successMessage = 'Redemption cancelled'; this.loadAll(); },
      error: (err) => { this.errorMessage = err.error?.message || 'Cancel failed'; }
    });
  }
}
