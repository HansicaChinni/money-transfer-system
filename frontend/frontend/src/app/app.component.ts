import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NavbarComponent } from './component/navbar-component/navbar-component.component';
import { AuthService } from './services/auth-service.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'Money Transfer System';
  showNavbar = false;

  constructor(
    private router: Router,
    private authService: AuthService
  ) {
    // Show navbar only when authenticated and not on login page
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.showNavbar = this.authService.isAuthenticated() && !event.url.includes('/login');
    });
  }
}