import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth-service.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Skip adding token for login requests
  if (req.url.includes('/auth/login')) {
    console.log('[AuthInterceptor] Skipping token for login request:', req.url);
    return next(req);
  }

  // Clone the request and add authorization header if token exists
  if (token) {
    console.log('[AuthInterceptor] Adding Bearer token to request:', req.url);
    const clonedRequest = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(clonedRequest);
  }

  console.warn('[AuthInterceptor] No token found for request:', req.url);
  return next(req);
};