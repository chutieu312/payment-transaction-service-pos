import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getTransactionsByAccount,
  type TransferResponse,
} from "../api/transactionApi";
import TransactionRow from "../components/TransactionRow";

const DEMO_ACCOUNT_ID = import.meta.env.VITE_ACCOUNT_ID ?? "";

export default function TransactionHistory() {
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState<TransferResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!DEMO_ACCOUNT_ID) return;
    getTransactionsByAccount(DEMO_ACCOUNT_ID)
      .then((data) => setTransactions(data as TransferResponse[]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow px-6 py-4 flex justify-between items-center">
        <button
          onClick={() => navigate("/")}
          className="text-sm text-gray-500 hover:text-brand-700"
        >
          ← Back
        </button>
        <span className="font-semibold text-gray-700">Transaction History</span>
        <span />
      </nav>

      <main className="max-w-4xl mx-auto py-8 px-4">
        {loading ? (
          <p className="text-gray-400 text-sm">Loading…</p>
        ) : transactions.length === 0 ? (
          <p className="text-gray-400 text-sm">No transactions found.</p>
        ) : (
          <div className="bg-white rounded-2xl shadow overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Amount</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {transactions.map((tx) => (
                  <TransactionRow
                    key={tx.id}
                    tx={tx}
                    currentAccountId={DEMO_ACCOUNT_ID}
                  />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
