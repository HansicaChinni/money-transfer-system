import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TransferRequest, TransferResponse } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class TransferService {
  private apiUrl = 'http://localhost:8080/api/v1/transfers';

  constructor(private http: HttpClient) {}

  executeTransfer(request: TransferRequest): Observable<TransferResponse> {
    return this.http.post<TransferResponse>(this.apiUrl, request);
  }

  generateIdempotencyKey(): string {
    return `${Date.now()}-${Math.random().toString(36).substring(2, 15)}`;
  }
}
