import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RewardSummaryResponse,
  RewardTransactionResponse,
  TransactionDetailResponse
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class RewardService {
  private apiUrl = 'http://localhost:8080/me/rewards';

  constructor(private http: HttpClient) {}

  getRewardSummary(): Observable<RewardSummaryResponse> {
    return this.http.get<RewardSummaryResponse>(`${this.apiUrl}/summary`);
  }

  getRewardTransactions(): Observable<RewardTransactionResponse[]> {
    return this.http.get<RewardTransactionResponse[]>(`${this.apiUrl}/transactions`);
  }
}
