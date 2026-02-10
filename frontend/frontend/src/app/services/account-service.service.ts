import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AccountResponse, TransactionLogResponse } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private readonly API_URL = 'http://localhost:8080/api/v1/accounts';

  constructor(private http: HttpClient) {}

  getAccount(id: number): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(`${this.API_URL}/${id}`);
  }

  getBalance(id: number): Observable<number> {
    return this.http.get<number>(`${this.API_URL}/${id}/balance`);
  }

  getTransactions(id: number): Observable<TransactionLogResponse[]> {
    return this.http.get<TransactionLogResponse[]>(`${this.API_URL}/${id}/transactions`);
  }
}