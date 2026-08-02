import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { clearToken, getBalance, type BalanceResponse } from "../api/transactionApi";
import BalanceCard from "../components/BalanceCard";
import TransferForm from "../components/TransferForm";

// NOTE: In a real app, the account ID would come from the JWT claims.
// For the demo, use Alice's seeded account ID (set via VITE_ALICE_ACCOUNT_ID or hardcoded).
const DEMO_ACCOUNT_ID = import.meta.env.VITE_ACCOUNT_ID ?? "";

export default function AccountDashboard() {
  const navigate = useNavigate();
  const [balance, setBalance] = useState<BalanceResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [showTransfer, setShowTransfer] = useState(false);

  const fetchBalance = async () => {
    if (!DEMO_ACCOUNT_ID) return;
    setLoading(true);
    try {
      const data = await getBalance(DEMO_ACCOUNT_ID);
      setBalance(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchBalance(); }, []);

  const handleLogout = () => {
    clearToken();
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow px-6 py-4 flex justify-between items-center">
        <span className="font-bold text-lg text-brand-700">Payment Service</span>
        <div className="flex gap-4">
          <button
            onClick={() => navigate("/transactions")}
            className="text-sm text-gray-600 hover:text-brand-700"
          >
            History
          </button>
          <button
            onClick={handleLogout}
            className="text-sm text-red-500 hover:text-red-700"
          >
            Logout
          </button>
        </div>
      </nav>

      <main className="max-w-lg mx-auto py-10 px-4 space-y-6">
        <BalanceCard balance={balance} loading={loading} />

        <button
          onClick={() => setShowTransfer((v) => !v)}
          className="w-full bg-brand-500 hover:bg-brand-700 text-white font-semibold py-2 rounded-lg"
        >
          {showTransfer ? "Cancel" : "New Transfer"}
        </button>

        {showTransfer && DEMO_ACCOUNT_ID && (
          <div className="bg-white rounded-2xl shadow p-6">
            <h2 className="text-lg font-semibold mb-4">Send Money</h2>
            <TransferForm
              fromAccountId={DEMO_ACCOUNT_ID}
              onSuccess={() => {
                setShowTransfer(false);
                fetchBalance();
              }}
            />
          </div>
        )}

        {!DEMO_ACCOUNT_ID && (
          <p className="text-sm text-amber-600 bg-amber-50 p-3 rounded-lg">
            Set <code>VITE_ACCOUNT_ID</code> in your <code>.env</code> to your account UUID.
          </p>
        )}
      </main>
    </div>
  );
}
