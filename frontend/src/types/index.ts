export type Role = "USER" | "ADMIN";

export interface User {
  id: string;
  email: string;
  role: Role;
  enabled: boolean;
  dailyLimit: number;
  createdAt: string;
  lastLoginAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  expiresIn: number;
  user: User;
}

export type RequestStatus =
  | "CREATED"
  | "VALIDATING"
  | "PROCESSING"
  | "REVIEW"
  | "QUEUED_FOR_SHOP"
  | "IMPORTED"
  | "FAILED";

export interface RequestOptions {
  generateWebText: boolean;
  generateSocialPost: boolean;
  generateModelPhoto: boolean;
}

export interface ShopRequest {
  id: string;
  description: string;
  options: RequestOptions;
  status: RequestStatus;
  errorMessage?: string;
  createdAt: string;
  finishedAt?: string;
}

export interface ProductText {
  title: string;
  shortDescription: string;
  longDescription: string;
  keywords: string[];
}

export interface SocialText {
  instagram: {
    caption: string;
    hashtags: string[];
  };
  facebook: {
    text: string;
  };
}

export interface RequestResult {
  requestId: string;
  webText?: ProductText;
  socialText?: SocialText;
  modelImageUrl?: string;
  wooJson?: unknown;
  createdAt: string;
}

export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface ApiErrorBody {
  status: number;
  code: string;
  message: string;
  traceId?: string;
}

export interface CreateRequestBody {
  photo: File;
  description: string;
  options: RequestOptions;
}

export interface ShopQueueItem {
  id: string;
  requestId: string;
  status: string;
  note?: string;
  createdAt: string;
  processedAt?: string;
}

export interface AuditEvent {
  id: string;
  userId?: string;
  action: string;
  ip?: string;
  details?: Record<string, unknown>;
  timestamp: string;
}

export interface Stats {
  requestsTotal: number;
  requestsToday: number;
  failedTotal: number;
  aiTokensTotal: number;
  activeUsers: number;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  role: Role;
  dailyLimit: number;
}

export interface UpdateUserRequest {
  role?: Role;
  enabled?: boolean;
  dailyLimit?: number;
}
