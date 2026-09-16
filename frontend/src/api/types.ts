export type Category =
  | 'GROCERIES' | 'RESTAURANT' | 'PHARMA' | 'HOUSING' | 'TRANSPORT' | 'AUTO'
  | 'SUBSCRIPTIONS' | 'CLOTHING' | 'PERSONAL_CARE' | 'LEISURE' | 'EDUCATION'
  | 'PETS' | 'TRAVEL' | 'GIFTS' | 'TAXES' | 'OTHER';

export type BudgetStatus = 'OK' | 'WARNING' | 'EXCEEDED';

export interface UserResponse {
  id: string;
  name: string;
  email: string;
  phone?: string | null;
}

export interface WhatsAppConnection {
  state: string;
  qrCodeBase64?: string | null;
  pairingCode?: string | null;
  connected: boolean;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface TransactionResponse {
  id: string;
  description: string;
  amount: number;
  category: Category;
  categoryLabel: string;
  date: string;
}

export interface TransactionRequest {
  description: string;
  amount: number;
  category: Category;
  date?: string;
}

export interface CategorySummary {
  category: Category;
  categoryLabel: string;
  total: number;
  quantity: number;
  percentage: number;
}

export interface SpendingSummary {
  start: string;
  end: string;
  total: number;
  quantity: number;
  categories: CategorySummary[];
}

export interface BudgetStatusResponse {
  id: string;
  category: Category;
  categoryLabel: string;
  month: string;
  monthlyLimit: number;
  spent: number;
  remaining: number;
  usedPercentage: number;
  status: BudgetStatus;
  statusLabel: string;
  message: string;
}

export interface AssistantResponse {
  transcription: string | null;
  answer: string;
  conversationId: string;
}

/** Formato de erro da API (RFC 9457) */
export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}
