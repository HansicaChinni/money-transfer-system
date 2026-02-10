import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminAccountView, TransactionLogResponse } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly API_URL = 'http://localhost:8080/admin';

  constructor(private http: HttpClient) {}

  getAllAccounts(): Observable<AdminAccountView[]> {
    return this.http.get<AdminAccountView[]>(`${this.API_URL}/accounts`);
  }

  getAllTransactions(): Observable<TransactionLogResponse[]> {
    return this.http.get<TransactionLogResponse[]>(`${this.API_URL}/transactions`);
  }
}