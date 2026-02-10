import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TransferRequest, TransferResponse, MeTransferRequest } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class TransferService {
  private readonly API_URL = 'http://localhost:8080/api/v1/transfers';
  private readonly ME_API_URL = 'http://localhost:8080/me/transfer';

  constructor(private http: HttpClient) {}

  transfer(request: TransferRequest): Observable<TransferResponse> {
    return this.http.post<TransferResponse>(this.API_URL, request);
  }

  transferForCurrentUser(request: MeTransferRequest): Observable<TransferResponse> {
    return this.http.post<TransferResponse>(this.ME_API_URL, request);
  }

  generateIdempotencyKey(): string {
    return `${Date.now()}-${Math.random().toString(36).substring(2, 15)}`;
  }
}