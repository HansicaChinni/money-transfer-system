import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, interval, Subscription } from 'rxjs';
import { CaptchaChallenge, LoginRequest, LoginResponse } from '../models/api.models';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080';
  private currentUserSubject = new BehaviorSubject<LoginResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  private static readonly SESSION_DURATION = 5 * 60 * 1000;
  private static readonly STORAGE_EXPIRY_KEY = 'sessionExpiry';
  private remainingSecondsSubject = new BehaviorSubject<number>(0);
  public remainingSeconds$ = this.remainingSecondsSubject.asObservable();
  private sessionTimerSub?: Subscription;

  getCaptcha(): Observable<CaptchaChallenge> {
    return this.http.get<CaptchaChallenge>(`${this.apiUrl}/auth/captcha`);
  }

  constructor(private http: HttpClient, private router: Router) {
    if (typeof localStorage !== 'undefined') {
      const saved = this.getUserFromStorage();
      if (saved) {
        const remaining = this.getRemainingSeconds();
        if (remaining > 0) {
          this.currentUserSubject.next(saved);
          this.startCountdown(remaining);
        } else {
          this.clearSession();
        }
      }
    }
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap(response => {
        localStorage.setItem('currentUser', JSON.stringify(response));
        this.setSessionExpiry();
        this.currentUserSubject.next(response);
        this.startCountdown();
      })
    );
  }

  logout(): void {
    this.clearSession();
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
    return !!this.getToken() && this.getRemainingSeconds() > 0;
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  isUser(): boolean {
    return this.getUserRole() === 'USER';
  }

  private getUserFromStorage(): LoginResponse | null {
    if (typeof localStorage === 'undefined') return null;
    const userStr = localStorage.getItem('currentUser');
    return userStr ? JSON.parse(userStr) : null;
  }

  getCurrentUser(): LoginResponse | null {
    return this.currentUserSubject.value;
  }

  private setSessionExpiry(): void {
    const expiry = Date.now() + AuthService.SESSION_DURATION;
    localStorage.setItem(AuthService.STORAGE_EXPIRY_KEY, String(expiry));
  }

  private getRemainingSeconds(): number {
    const expiryStr = localStorage.getItem(AuthService.STORAGE_EXPIRY_KEY);
    if (!expiryStr) return 0;
    const remaining = parseInt(expiryStr, 10) - Date.now();
    return Math.max(0, Math.floor(remaining / 1000));
  }

  private startCountdown(initial?: number): void {
    this.stopCountdown();
    this.remainingSecondsSubject.next(initial ?? AuthService.SESSION_DURATION / 1000);
    this.sessionTimerSub = interval(1000).subscribe(() => {
      const remaining = this.getRemainingSeconds();
      this.remainingSecondsSubject.next(remaining);
      if (remaining <= 0) {
        this.stopCountdown();
        this.clearSession();
        this.router.navigate(['/login']);
      }
    });
  }

  private stopCountdown(): void {
    this.sessionTimerSub?.unsubscribe();
    this.sessionTimerSub = undefined;
  }

  private clearSession(): void {
    this.stopCountdown();
    this.remainingSecondsSubject.next(0);
    localStorage.removeItem('currentUser');
    localStorage.removeItem(AuthService.STORAGE_EXPIRY_KEY);
    this.currentUserSubject.next(null);
  }
}
