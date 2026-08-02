import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import AccountDashboard from "./pages/AccountDashboard";
import TransactionHistory from "./pages/TransactionHistory";
import AdminPanel from "./pages/AdminPanel";
import { getToken } from "./api/transactionApi";

function RequireAuth({ children }: { children: React.ReactNode }) {
  return getToken() ? <>{children}</> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <AccountDashboard />
            </RequireAuth>
          }
        />
        <Route
          path="/transactions"
          element={
            <RequireAuth>
              <TransactionHistory />
            </RequireAuth>
          }
        />
        <Route
          path="/admin"
          element={
            <RequireAuth>
              <AdminPanel />
            </RequireAuth>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}
