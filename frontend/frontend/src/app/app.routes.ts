import { Routes } from '@angular/router';
import { LoginComponent } from './component/login-component/login-component.component';
import { DashboardComponent } from './component/dashboard-component/dashboard-component.component';
import { TransferComponent } from './component/transfer-component/transfer-component.component';
import { HistoryComponent } from './component/history-component/history-component.component';
import { AdminComponent } from './component/admin-component/admin-component.component';
import { authGuard } from './guards/auth-guard.guard';
import { adminGuard } from './guards/admin-guard.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { 
    path: 'dashboard', 
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  { 
    path: 'transfer', 
    component: TransferComponent,
    canActivate: [authGuard]
  },
  { 
    path: 'history', 
    component: HistoryComponent,
    canActivate: [authGuard]
  },
  { 
    path: 'admin', 
    component: AdminComponent,
    canActivate: [authGuard, adminGuard]
  },
  { path: '**', redirectTo: '/login' }
];