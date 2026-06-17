import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdminAccountView,
  AdminAccountDetailResponse,
  TransactionLogResponse,
  AdminCreateAccountRequest,
  AccountStatus,
  RedemptionResponse,
  RewardItemResponse,
  CreateRewardItemRequest,
  FulfillRequest
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = 'http://localhost:8080/admin';
  private rewardUrl = 'http://localhost:8080/api/admin/rewards';

  constructor(private http: HttpClient) {}

  getAllAccounts(): Observable<AdminAccountView[]> {
    return this.http.get<AdminAccountView[]>(`${this.apiUrl}/accounts`);
  }

  getAccountDetails(id: number): Observable<AdminAccountDetailResponse> {
    return this.http.get<AdminAccountDetailResponse>(`${this.apiUrl}/accounts/${id}`);
  }

  getAccountTransactions(id: number): Observable<TransactionLogResponse[]> {
    return this.http.get<TransactionLogResponse[]>(`${this.apiUrl}/transactions/${id}`);
  }

  createAccount(request: AdminCreateAccountRequest): Observable<AdminAccountDetailResponse> {
    return this.http.post<AdminAccountDetailResponse>(`${this.apiUrl}/accounts`, request);
  }

  updateAccountStatus(id: number, status: AccountStatus): Observable<AdminAccountDetailResponse> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<AdminAccountDetailResponse>(`${this.apiUrl}/accounts/${id}/status`, null, { params });
  }

  getAllTransactions(): Observable<TransactionLogResponse[]> {
    return this.http.get<TransactionLogResponse[]>(`${this.apiUrl}/transactions`);
  }

  getRedemptions(): Observable<RedemptionResponse[]> {
    return this.http.get<RedemptionResponse[]>(`${this.rewardUrl}/redemptions`);
  }

  fulfillRedemption(id: number, request: FulfillRequest): Observable<RedemptionResponse> {
    return this.http.patch<RedemptionResponse>(`${this.rewardUrl}/redemptions/${id}/fulfill`, request);
  }

  cancelRedemption(id: number): Observable<RedemptionResponse> {
    return this.http.patch<RedemptionResponse>(`${this.rewardUrl}/redemptions/${id}/cancel`, {});
  }

  getRewardItems(): Observable<RewardItemResponse[]> {
    return this.http.get<RewardItemResponse[]>(`${this.rewardUrl}/items`);
  }

  createRewardItem(request: CreateRewardItemRequest): Observable<RewardItemResponse> {
    return this.http.post<RewardItemResponse>(`${this.rewardUrl}/items`, request);
  }

  updateRewardItem(id: number, request: CreateRewardItemRequest): Observable<RewardItemResponse> {
    return this.http.put<RewardItemResponse>(`${this.rewardUrl}/items/${id}`, request);
  }

  deleteRewardItem(id: number): Observable<void> {
    return this.http.delete<void>(`${this.rewardUrl}/items/${id}`);
  }
}
