import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { userGuard } from './core/guards/user.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'user',
    canActivate: [authGuard, userGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/user/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'transfer',
        loadComponent: () => import('./features/user/transfer/transfer.component').then(m => m.TransferComponent)
      },
      {
        path: 'transactions',
        loadComponent: () => import('./features/user/transactions/transactions.component').then(m => m.TransactionsComponent)
      },
      {
        path: 'rewards',
        loadComponent: () => import('./features/user/rewards/rewards.component').then(m => m.RewardsComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/user/profile/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/admin/dashboard/dashboard.component').then(m => m.AdminDashboardComponent)
      },
      {
        path: 'accounts',
        loadComponent: () => import('./features/admin/accounts/accounts.component').then(m => m.AccountsComponent)
      },
      {
        path: 'accounts/:id',
        loadComponent: () => import('./features/admin/account-detail/account-detail.component').then(m => m.AccountDetailComponent)
      },
      {
        path: 'transactions',
        loadComponent: () => import('./features/admin/transactions/transactions.component').then(m => m.AdminTransactionsComponent)
      },
      {
        path: 'rewards',
        loadComponent: () => import('./features/admin/rewards/rewards.component').then(m => m.AdminRewardsComponent)
      },
      {
        path: 'create-account',
        loadComponent: () => import('./features/admin/create-account/create-account.component').then(m => m.CreateAccountComponent)
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
