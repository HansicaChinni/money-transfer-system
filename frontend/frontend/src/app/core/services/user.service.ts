import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AccountResponse,
  TransactionLogResponse,
  MeTransferRequest,
  TransferResponse,
  ChangePasswordRequest,
  RewardSummaryResponse,
  RewardTransactionResponse,
  RewardItemResponse,
  RedeemRequest,
  RedemptionResponse
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = 'http://localhost:8080/me';
  private rewardUrl = 'http://localhost:8080/api/user/rewards';

  constructor(private http: HttpClient) {}

  getBalance(): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(`${this.apiUrl}/balance`);
  }

  getTransactions(): Observable<TransactionLogResponse[]> {
    return this.http.get<TransactionLogResponse[]>(`${this.apiUrl}/transactions`);
  }

  transfer(request: MeTransferRequest): Observable<TransferResponse> {
    return this.http.post<TransferResponse>(`${this.apiUrl}/transfer`, request);
  }

  changePassword(request: ChangePasswordRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/password`, request);
  }

  getRewardSummary(): Observable<RewardSummaryResponse> {
    return this.http.get<RewardSummaryResponse>(`${this.rewardUrl}/summary`);
  }

  getRewardHistory(): Observable<RewardTransactionResponse[]> {
    return this.http.get<RewardTransactionResponse[]>(`${this.rewardUrl}/history`);
  }

  getRewardStore(): Observable<RewardItemResponse[]> {
    return this.http.get<RewardItemResponse[]>(`${this.rewardUrl}/store`);
  }

  redeem(request: RedeemRequest): Observable<RedemptionResponse> {
    return this.http.post<RedemptionResponse>(`${this.rewardUrl}/redeem`, request);
  }

  getRedemptions(): Observable<RedemptionResponse[]> {
    return this.http.get<RedemptionResponse[]>(`${this.rewardUrl}/redemptions`);
  }
}
