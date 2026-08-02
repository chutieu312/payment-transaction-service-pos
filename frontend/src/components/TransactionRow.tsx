import type { TransferResponse } from "../api/transactionApi";
import StatusBadge from "./StatusBadge";

interface TransactionRowProps {
  tx: TransferResponse;
  currentAccountId: string;
}

export default function TransactionRow({ tx, currentAccountId }: TransactionRowProps) {
  const isDebit = tx.fromAccountId === currentAccountId;
  const sign = isDebit ? "-" : "+";
  const color = isDebit ? "text-red-600" : "text-green-600";

  return (
    <tr className="hover:bg-gray-50">
      <td className="px-4 py-3 text-sm text-gray-500 whitespace-nowrap">
        {new Date(tx.createdAt).toLocaleString()}
      </td>
      <td className="px-4 py-3 text-sm text-gray-700">{tx.description ?? "—"}</td>
      <td className={`px-4 py-3 text-sm font-semibold ${color}`}>
        {sign}
        {tx.currency} {Number(tx.amount).toLocaleString("en-US", { minimumFractionDigits: 2 })}
      </td>
      <td className="px-4 py-3">
        <StatusBadge status={tx.status} />
      </td>
    </tr>
  );
}
