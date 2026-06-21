export interface CaptchaChallenge {
  token: string;
  question: string;
  hint: string;
}

export interface LoginRequest {
  username: string;
  password: string;
  captchaToken?: string;
  captchaAnswer?: string;
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
  dailyTransferLimit: number;
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
  dailyTransferLimit: number;
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
}

export interface AdminCreateAccountRequest {
  username: string;
  password: string;
  holderName: string;
  initialBalance: number;
  captchaToken?: string;
  captchaAnswer?: string;
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

export interface RewardSummaryResponse {
  totalPoints: number;
  lastEarned: number | null;
  lastEarnedOn: string | null;
}

export interface RewardTransactionResponse {
  id: number;
  transactionId: number;
  pointsEarned: number;
  amount: number;
  reason: string;
  createdOn: string;
}

export interface RewardItemResponse {
  id: number;
  name: string;
  description: string;
  brand: string;
  pointsRequired: number;
  couponValue: number;
  isActive: boolean;
  imageUrl: string | null;
}

export interface RedeemRequest {
  rewardItemId: number;
}

export interface RedemptionResponse {
  id: number;
  rewardItemId: number;
  itemName: string;
  brand: string;
  pointsSpent: number;
  couponValue: number;
  status: string;
  couponCode: string | null;
  notes: string | null;
  createdOn: string;
  fulfilledOn: string | null;
}

export interface CreateRewardItemRequest {
  name: string;
  description: string;
  brand: string;
  pointsRequired: number;
  couponValue: number;
  imageUrl: string | null;
}

export interface FulfillRequest {
  notes: string;
}

export interface RewardRatioResponse {
  pointsPerUnit: number;
}
