// Authentication Models
export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  role: string;
  accountId: number | null;
}

// Account Models
export interface AccountResponse {
  id: number;
  holderName: string;
  balance: number;
  status: string;
  lastUpdated?: string;
}

export enum AccountStatus {
  ACTIVE = 'ACTIVE',
  LOCKED = 'LOCKED',
  CLOSED = 'CLOSED'
}

// Transfer Models
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
  message?: string;
  transactionId?: number;
  amount: number;
}

// Transaction Models
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

export enum TransactionStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED'
}

// Admin Models
export interface AdminAccountView {
  id: number;
  balance: number;
  status: string;
  lastUpdated: string;
}

// Error Response
export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

// User info from JWT
export interface UserInfo {
  username: string;
  role: string;
  accountId: number | null;
}