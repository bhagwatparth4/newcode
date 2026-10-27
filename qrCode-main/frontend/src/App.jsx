import { Navigate, Route, Routes } from 'react-router-dom'
import MenuPage from './customer/MenuPage'
import CartPage from './customer/CartPage'
import OrderPage from './customer/OrderPage'
import ScanPrompt from './customer/ScanPrompt'
import LoginPage from './auth/LoginPage'
import RequireRole from './auth/RequireRole'
import AdminLayout from './admin/AdminLayout'
import DashboardPage from './admin/DashboardPage'
import LiveOrdersPage from './admin/LiveOrdersPage'
import OrdersPage from './admin/OrdersPage'
import MenuAdminPage from './admin/MenuAdminPage'
import TablesPage from './admin/TablesPage'
import CouponsPage from './admin/CouponsPage'
import CustomersPage from './admin/CustomersPage'
import SettingsPage from './admin/SettingsPage'
import KitchenBoard from './kitchen/KitchenBoard'
import TableStatusPage from './admin/TableStatusPage'
import DayReportPage from './admin/DayReportPage'
import FeedbackPage from './admin/FeedbackPage'
import CounterOrderPage from './admin/CounterOrderPage'
/*
 * Customer:  /menu?qr=TOKEN  ->  /cart  ->  /order/:token
 * Staff:     /login  ->  /admin/...  (ADMIN)  or  /kitchen  (KITCHEN)
 */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<ScanPrompt />} />
      <Route path="/menu" element={<MenuPage />} />
      <Route path="/cart" element={<CartPage />} />
      <Route path="/order/:token" element={<OrderPage />} />

      <Route path="/login" element={<LoginPage />} />

      <Route
        path="/admin"
        element={
          <RequireRole role="ADMIN">
            <AdminLayout />
          </RequireRole>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="live" element={<LiveOrdersPage />} />
        <Route path="new-order" element={<CounterOrderPage />} />
        <Route path="orders" element={<OrdersPage />} />
        <Route path="menu" element={<MenuAdminPage />} />
        <Route path="tables" element={<TablesPage />} />
        <Route path="table-status" element={<TableStatusPage />} />
        <Route path="day-report" element={<DayReportPage />} />
        <Route path="feedback" element={<FeedbackPage />} />
        <Route path="coupons" element={<CouponsPage />} />
        <Route path="customers" element={<CustomersPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>

      <Route
        path="/kitchen"
        element={
          <RequireRole role="KITCHEN">
            <KitchenBoard />
          </RequireRole>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
