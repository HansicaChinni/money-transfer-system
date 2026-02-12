import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { LoginRequest, LoginResponse } from '../models/api.models';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080';
  private currentUserSubject = new BehaviorSubject<LoginResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    // Initialize user from storage in browser environment only
    if (typeof localStorage !== 'undefined') {
      this.currentUserSubject.next(this.getUserFromStorage());
    }
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap(response => {
        localStorage.setItem('currentUser', JSON.stringify(response));
        this.currentUserSubject.next(response);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    const user = this.getUserFromStorage();
    return user ? user.token : null;
  }

  getUserRole(): string | null {
    const user = this.getUserFromStorage();
    return user ? user.role : null;
  }

  getAccountId(): number | null {
    const user = this.getUserFromStorage();
    return user ? user.accountId : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  isUser(): boolean {
    return this.getUserRole() === 'USER';
  }

  private getUserFromStorage(): LoginResponse | null {
    if (typeof localStorage === 'undefined') {
      return null;
    }
    const userStr = localStorage.getItem('currentUser');
    return userStr ? JSON.parse(userStr) : null;
  }

  getCurrentUser(): LoginResponse | null {
    return this.currentUserSubject.value;
  }
}
