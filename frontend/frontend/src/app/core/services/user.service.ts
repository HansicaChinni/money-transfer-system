import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AccountResponse,
  TransactionLogResponse,
  MeTransferRequest,
  TransferResponse,
  ChangePasswordRequest
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = 'http://localhost:8080/me';

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
}
