import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { ChangePasswordRequest, LoginRequest, LoginResponse, UserInfo } from '../models/models';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/auth';
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_INFO_KEY = 'user_info';
  
  private currentUserSubject = new BehaviorSubject<UserInfo | null>(this.getUserInfo());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API_URL}/login`, credentials)
      .pipe(
        tap(response => {
          this.setToken(response.token);
          const respAny = response as any;
          const accountId = respAny.accountId ?? respAny.account_id ?? null;
          const userInfo: UserInfo = {
            username: credentials.username,
            role: respAny.role ?? respAny.userRole ?? 'USER',
            accountId: accountId
          };
          this.setUserInfo(userInfo);
          this.currentUserSubject.next(userInfo);
        })
      );
  }

  changePassword(payload: ChangePasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>('http://localhost:8080/me/password', payload);
  }

  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.USER_INFO_KEY);
    }
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private setToken(token: string): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.TOKEN_KEY, token);
    }
  }

  getUserInfo(): UserInfo | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    const userInfoStr = localStorage.getItem(this.USER_INFO_KEY);
    return userInfoStr ? JSON.parse(userInfoStr) : null;
  }

  private setUserInfo(userInfo: UserInfo): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.USER_INFO_KEY, JSON.stringify(userInfo));
    }
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    const userInfo = this.getUserInfo();
    return userInfo?.role === 'ADMIN';
  }

  isUser(): boolean {
    const userInfo = this.getUserInfo();
    return userInfo?.role === 'USER';
  }

  getCurrentAccountId(): number | null {
    const userInfo = this.getUserInfo();
    return userInfo?.accountId || null;
  }
}