import { Component,OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth-service.service';
import { Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';



@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './login-component.component.html',
  styleUrls: ['./login-component.component.css']
})

export class LoginComponent {
  loginForm: FormGroup;
  loading = false;
  errorMessage = '';
  hidePassword = true;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
      @Inject(PLATFORM_ID) private platformId: Object

  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

ngOnInit(): void {
  if (isPlatformBrowser(this.platformId)) {

    // lock back/forward navigation after logout
    window.history.pushState(null, '', window.location.href);

    window.onpopstate = () => {
      window.history.pushState(null, '', window.location.href);
    };

    // existing token check
    if (localStorage.getItem('token')) {
      this.router.navigateByUrl('/dashboard', { replaceUrl: true });
    }
  }
}




  onSubmit(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        this.loading = false;
        // Navigate based on role
        if (response.role === 'ADMIN') {
this.router.navigateByUrl('/admin', { replaceUrl: true });
        } else {
this.router.navigateByUrl('/dashboard', { replaceUrl: true });
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Invalid username or password';
      }
    });
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  onLogoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img) {
      img.style.display = 'none';
    }
  }
}