const STATUS_STYLES: Record<string, string> = {
  PENDING_FRAUD_CHECK: "bg-yellow-100 text-yellow-800",
  PROCESSING: "bg-blue-100 text-blue-800",
  COMPLETED: "bg-green-100 text-green-800",
  FAILED: "bg-red-100 text-red-800",
  FRAUD_REJECTED: "bg-red-200 text-red-900",
  REVERSED: "bg-gray-100 text-gray-700",
};

interface StatusBadgeProps {
  status: string;
}

export default function StatusBadge({ status }: StatusBadgeProps) {
  const cls = STATUS_STYLES[status] ?? "bg-gray-100 text-gray-600";
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${cls}`}>
      {status.replace(/_/g, " ")}
    </span>
  );
}
