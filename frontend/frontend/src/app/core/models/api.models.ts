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
  accountNumber: string;
  holderName: string;
  balance: number;
  status: string;
  rewardPoints: number;
}

export interface AdminAccountView {
  id: number;
  accountNumber: string;
  holderName: string;
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
  rewardPoints: number;
  totalRewardsRedeemed: number;
}

export interface TransactionLogResponse {
  id: number;
  fromAccountId: number;
  toAccountId: number;
  fromAccountNumber: string;
  toAccountNumber: string;
  fromHolderName: string;
  toHolderName: string;
  amount: number;
  status: string;
  failureReason: string | null;
  idempotencyKey: string;
  createdOn: string;
  rewardPointsEarned: number | null;
  rewardPointsUsed: number | null;
}

export interface TransferRequest {
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  idempotencyKey: string;
  useRewardPoints: boolean;
}

export interface MeTransferRequest {
  toAccountNumber: string;
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
  accountNumber: string;
  holderName: string;
  type: string;
  points: number;
  referenceTransactionId: number | null;
  createdOn: string;
  expiresOn: string | null;
}

export interface RewardSummaryResponse {
  currentPoints: number;
  totalEarned: number;
  totalRedeemed: number;
  expiringPoints: number;
}

export interface AdminRewardDashboardResponse {
  totalPointsEarned: number;
  totalPointsRedeemed: number;
}

export interface TransactionDetailResponse {
  id: number;
  fromAccountId: number;
  toAccountId: number;
  fromAccountNumber: string;
  toAccountNumber: string;
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
