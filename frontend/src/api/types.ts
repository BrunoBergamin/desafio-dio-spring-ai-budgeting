export type ExpenseCategory =
  | 'GROCERIES' | 'RESTAURANT' | 'PHARMA' | 'HOUSING' | 'TRANSPORT' | 'AUTO'
  | 'SUBSCRIPTIONS' | 'CLOTHING' | 'PERSONAL_CARE' | 'LEISURE' | 'EDUCATION'
  | 'PETS' | 'TRAVEL' | 'GIFTS' | 'TAXES' | 'OTHER';

export type IncomeCategory = 'SALARY' | 'FREELANCE' | 'INVESTMENTS' | 'OTHER_INCOME';

export type Category = ExpenseCategory | IncomeCategory;

/** A categoria ja diz se o lancamento e gasto ou receita; o back-end devolve isto pronto. */
export type TransactionType = 'EXPENSE' | 'INCOME';

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
  type: TransactionType;
  /** Id da conta recorrente que gerou este lancamento; nulo quando foi lancado a mao */
  recurringId?: string | null;
  date: string;
}

/** Pagina de resultados da API (listas grandes vem em partes) */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TransactionRequest {
  description: string;
  amount: number;
  category: Category;
  date?: string;
}

export interface RecurringResponse {
  id: string;
  description: string;
  amount: number;
  category: Category;
  categoryLabel: string;
  type: TransactionType;
  dayOfMonth: number;
  active: boolean;
  startMonth: string;
  endMonth?: string | null;
  /** Proxima data em que vira lancamento; nulo quando pausada ou ja acabou */
  nextOccurrence?: string | null;
  message: string;
}

export interface RecurringRequest {
  description: string;
  amount: number;
  category: Category;
  dayOfMonth: number;
  startMonth?: string;
  endMonth?: string | null;
  active?: boolean;
}

export type GoalStatus = 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE';

export interface SavingsGoalResponse {
  id: string;
  name: string;
  targetAmount: number;
  savedAmount: number;
  remaining: number;
  percentage: number;
  deadline?: string | null;
  monthsLeft?: number | null;
  suggestedMonthly?: number | null;
  status: GoalStatus;
  statusLabel: string;
  message: string;
}

export interface GoalRequest {
  name: string;
  targetAmount: number;
  deadline?: string;
  savedAmount?: number;
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
  /** Total de gastos no periodo */
  total: number;
  /** Total recebido no periodo */
  income: number;
  /** income - total; negativo quando gastou mais do que entrou */
  balance: number;
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
