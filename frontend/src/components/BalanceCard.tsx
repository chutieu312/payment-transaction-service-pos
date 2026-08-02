import type { BalanceResponse } from "../api/transactionApi";

interface BalanceCardProps {
  balance: BalanceResponse | null;
  loading: boolean;
}

export default function BalanceCard({ balance, loading }: BalanceCardProps) {
  if (loading) {
    return (
      <div className="bg-white rounded-2xl shadow p-6 animate-pulse h-32" />
    );
  }
  if (!balance) return null;

  return (
    <div className="bg-gradient-to-r from-brand-700 to-brand-500 rounded-2xl shadow p-6 text-white">
      <p className="text-sm opacity-75">Account {balance.accountNumber}</p>
      <p className="text-4xl font-bold mt-1">
        {balance.currency}{" "}
        {Number(balance.balance).toLocaleString("en-US", {
          minimumFractionDigits: 2,
        })}
      </p>
      <p className="text-xs mt-3 opacity-60">Available balance</p>
    </div>
  );
}
