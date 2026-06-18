import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CaptchaChallenge } from '../../../core/models/api.models';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  loading = false;
  errorMessage = '';
  captcha: CaptchaChallenge | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: [
        '',
        [
          Validators.required,
          Validators.pattern(/^[A-Za-z0-9]+$/)
        ]
      ],
      password: [
        '',
        [
          Validators.required
        ]
      ],
      captchaAnswer: [
        '',
        [
          Validators.required
        ]
      ]
    });
  }

  ngOnInit(): void {
    this.loadCaptcha();
  }

  loadCaptcha(): void {
    this.authService.getCaptcha().subscribe({
      next: (c) => {
        this.captcha = c;
        this.loginForm.patchValue({ captchaAnswer: '' });
      },
      error: () => {
        this.errorMessage = 'Failed to load captcha. Please refresh.';
      }
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid || !this.captcha) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const credentials = {
      ...this.loginForm.value,
      captchaToken: this.captcha.token,
      captchaAnswer: this.loginForm.value.captchaAnswer
    };

    this.authService.login(credentials).subscribe({
      next: (response) => {
        if (response.role === 'ADMIN') {
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.router.navigate(['/user/dashboard']);
        }
      },
      error: (error) => {
        this.loading = false;
        if (error.error?.code === 'INVALID_CAPTCHA') {
          this.errorMessage = 'Incorrect captcha answer. Please try again.';
          this.loadCaptcha();
        } else {
          this.errorMessage = error.error?.message || 'Invalid username or password';
        }
      }
    });
  }
}
