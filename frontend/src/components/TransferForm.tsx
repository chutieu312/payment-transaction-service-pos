import { useState } from "react";
import { initiateTransfer, type TransferRequest } from "../api/transactionApi";
import { v4 as uuidv4 } from "uuid";

interface TransferFormProps {
  fromAccountId: string;
  onSuccess: () => void;
}

export default function TransferForm({ fromAccountId, onSuccess }: TransferFormProps) {
  const [toAccountId, setToAccountId] = useState("");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const payload: TransferRequest = {
        toAccountId,
        amount: parseFloat(amount),
        currency: "USD",
        idempotencyKey: uuidv4(),
        description,
      };
      await initiateTransfer(fromAccountId, payload);
      setToAccountId("");
      setAmount("");
      setDescription("");
      onSuccess();
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Transfer failed. Please try again.";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700">To Account ID</label>
        <input
          type="text"
          required
          value={toAccountId}
          onChange={(e) => setToAccountId(e.target.value)}
          placeholder="e.g. uuid-..."
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-brand-500 focus:border-brand-500"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700">Amount (USD)</label>
        <input
          type="number"
          required
          min="0.01"
          step="0.01"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-brand-500 focus:border-brand-500"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700">Description (optional)</label>
        <input
          type="text"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
        />
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <button
        type="submit"
        disabled={loading}
        className="w-full bg-brand-500 hover:bg-brand-700 text-white font-semibold py-2 px-4 rounded-lg disabled:opacity-50"
      >
        {loading ? "Sending…" : "Send Transfer"}
      </button>
    </form>
  );
}
