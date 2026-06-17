export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  role: string;
  accountId: number;
}

export interface AccountResponse {
  id: number;
  holderName: string;
  balance: number;
  status: string;
}

export interface AdminAccountView {
  id: number;
  accountNumber: string;
  balance: number;
  status: string;
  lastUpdated: string;
}

export interface AdminAccountDetailResponse {
  id: number;
  accountNumber: string;
  holderName: string;
  balance: number;
  status: string;
  version: number;
  lastUpdated: string;
}

export interface TransactionLogResponse {
  id: number;
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  status: string;
  failureReason: string | null;
  idempotencyKey: string;
  createdOn: string;
}

export interface TransferRequest {
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  idempotencyKey: string;
}

export interface MeTransferRequest {
  toAccountId: number;
  amount: number;
}

export interface TransferResponse {
  status: string;
  message: string | null;
  transactionId: number | null;
  amount: number | null;
  rewardPoints: number;
}

export interface RewardLogResponse {
  id: number;
  transactionId: number;
  accountId: number;
  transactionAmount: number;
  points: number;
  eligibilityReason: string;
  createdOn: string;
}

export interface AdminCreateAccountRequest {
  username: string;
  password: string;
  holderName: string;
  initialBalance: number;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ErrorResponse {
  code: string;
  message: string;
  path: string;
  timestamp: string;
}

export enum AccountStatus {
  ACTIVE = 'ACTIVE',
  LOCKED = 'LOCKED',
  CLOSED = 'CLOSED'
}

export enum Role {
  USER = 'USER',
  ADMIN = 'ADMIN'
}
