import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { AccountResponse, TransactionLogResponse } from '../models/models';
import { AuthService } from './auth-service.service';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private readonly API_URL = 'http://localhost:8080/me';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  /**
   * Get current user's account information
   * Retrieves balance from /me/balance and account ID from JWT token
   */
  getAccount(): Observable<AccountResponse> {
    const accountId = this.authService.getCurrentAccountId() || 0;
    
    return this.http.get<{ balance: number }>(`${this.API_URL}/balance`).pipe(
      map(response => ({
        id: accountId,
        holderName: 'Your Account',
        balance: response.balance,
        status: 'ACTIVE',
        lastUpdated: new Date().toISOString()
      }))
    );
  }

  /**
   * Get current user's account balance
   * Server derives user context from JWT token
   */
  getBalance(): Observable<{ balance: number }> {
    return this.http.get<{ balance: number }>(`${this.API_URL}/balance`);
  }

  /**
   * Get current user's transaction history
   * Server derives user context from JWT token
   */
  getTransactions(): Observable<TransactionLogResponse[]> {
    return this.http.get<TransactionLogResponse[]>(`${this.API_URL}/transactions`);
  }
}