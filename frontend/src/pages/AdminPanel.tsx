import { useEffect, useState } from "react";
import { getAllTransactions, reverseTransaction, type TransferResponse } from "../api/transactionApi";
import { useNavigate } from "react-router-dom";
import StatusBadge from "../components/StatusBadge";

export default function AdminPanel() {
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState<TransferResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAll = () => {
    setLoading(true);
    getAllTransactions()
      .then(setTransactions)
      .catch(() => setError("Access denied or failed to load."))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchAll(); }, []);

  const handleReverse = async (id: string) => {
    try {
      await reverseTransaction(id);
      fetchAll();
    } catch {
      alert("Reversal failed.");
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow px-6 py-4 flex justify-between items-center">
        <button onClick={() => navigate("/")} className="text-sm text-gray-500 hover:text-brand-700">
          ← Back
        </button>
        <span className="font-semibold text-gray-700">Admin Panel — All Transactions</span>
        <span />
      </nav>

      <main className="max-w-6xl mx-auto py-8 px-4">
        {error && <p className="text-red-600 text-sm mb-4">{error}</p>}
        {loading ? (
          <p className="text-gray-400 text-sm">Loading…</p>
        ) : (
          <div className="bg-white rounded-2xl shadow overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Amount</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {transactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-gray-500 font-mono text-xs">{tx.id.slice(0, 8)}…</td>
                    <td className="px-4 py-3 text-gray-700 font-semibold">
                      {tx.currency} {Number(tx.amount).toLocaleString("en-US", { minimumFractionDigits: 2 })}
                    </td>
                    <td className="px-4 py-3"><StatusBadge status={tx.status} /></td>
                    <td className="px-4 py-3 text-gray-500">{new Date(tx.createdAt).toLocaleString()}</td>
                    <td className="px-4 py-3">
                      {tx.status === "COMPLETED" && (
                        <button
                          onClick={() => handleReverse(tx.id)}
                          className="text-xs text-red-600 hover:underline"
                        >
                          Reverse
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
