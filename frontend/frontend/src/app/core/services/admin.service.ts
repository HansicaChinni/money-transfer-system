import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdminAccountView,
  AdminAccountDetailResponse,
  AdminRewardDashboardResponse,
  TransactionDetailResponse,
  TransactionLogResponse,
  AdminCreateAccountRequest,
  AccountStatus,
  RewardTransactionResponse
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = 'http://localhost:8080/admin';

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

  getRewardDashboard(): Observable<AdminRewardDashboardResponse> {
    return this.http.get<AdminRewardDashboardResponse>(`${this.apiUrl}/rewards/dashboard`);
  }

  getAllRewardTransactions(): Observable<RewardTransactionResponse[]> {
    return this.http.get<RewardTransactionResponse[]>(`${this.apiUrl}/rewards`);
  }

  getTransactionDetail(transactionId: number): Observable<TransactionDetailResponse> {
    return this.http.get<TransactionDetailResponse>(`${this.apiUrl}/transactions/detail/${transactionId}`);
  }
}
