import axios from "axios";

const api = axios.create({ baseURL: "/api" });

// Attach JWT on every request
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ── Auth ──────────────────────────────────────────────────────────────────────
export function getToken(): string | null {
  return localStorage.getItem("jwt");
}

export function saveToken(token: string): void {
  localStorage.setItem("jwt", token);
}

export function clearToken(): void {
  localStorage.removeItem("jwt");
}

export async function login(email: string, password: string): Promise<string> {
  const { data } = await api.post<{ token: string }>("/auth/login", { email, password });
  return data.token;
}

export async function register(
  email: string,
  password: string,
  role: string
): Promise<string> {
  const { data } = await api.post<{ token: string }>("/auth/register", {
    email,
    password,
    role,
  });
  return data.token;
}

// ── Accounts ─────────────────────────────────────────────────────────────────
export interface BalanceResponse {
  accountId: string;
  accountNumber: string;
  balance: number;
  currency: string;
}

export async function getBalance(accountId: string): Promise<BalanceResponse> {
  const { data } = await api.get<BalanceResponse>(`/accounts/${accountId}/balance`);
  return data;
}

export async function getTransactionsByAccount(accountId: string) {
  const { data } = await api.get(`/accounts/${accountId}/transactions`);
  return data;
}

// ── Transfers ─────────────────────────────────────────────────────────────────
export interface TransferRequest {
  toAccountId: string;
  amount: number;
  currency: string;
  idempotencyKey: string;
  description?: string;
}

export interface TransferResponse {
  id: string;
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  currency: string;
  status: string;
  idempotencyKey: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export async function initiateTransfer(
  fromAccountId: string,
  payload: TransferRequest
): Promise<TransferResponse> {
  const { data } = await api.post<TransferResponse>(
    `/transfers/${fromAccountId}`,
    payload
  );
  return data;
}

export async function getAllTransactions(): Promise<TransferResponse[]> {
  const { data } = await api.get<TransferResponse[]>("/transfers");
  return data;
}

export async function reverseTransaction(transactionId: string): Promise<TransferResponse> {
  const { data } = await api.post<TransferResponse>(`/transfers/${transactionId}/reverse`);
  return data;
}

export default api;
