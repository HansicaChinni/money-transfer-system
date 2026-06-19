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
  rewardPoints: number;
}

export interface AdminAccountView {
  id: number;
  balance: number;
  status: string;
  lastUpdated: string;
}

export interface AdminAccountDetailResponse {
  id: number;
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
  useRewardPoints: boolean;
}

export interface MeTransferRequest {
  toAccountId: number;
  amount: number;
  useRewardPoints: boolean;
}

export interface TransferResponse {
  status: string;
  message: string | null;
  transactionId: number | null;
  amount: number | null;
  rewardPointsUsed: number | null;
  rewardPointsEarned: number | null;
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

export interface RewardTransactionResponse {
  id: number;
  accountId: number;
  type: string;
  points: number;
  referenceTransactionId: number | null;
  createdOn: string;
}

export interface RewardSummaryResponse {
  currentPoints: number;
  totalEarned: number;
  totalRedeemed: number;
}

export interface TransactionDetailResponse {
  id: number;
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  status: string;
  failureReason: string | null;
  idempotencyKey: string;
  createdOn: string;
  rewardPointsEarned: number | null;
  rewardPointsUsed: number | null;
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
