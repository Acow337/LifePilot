import { Navigate, createBrowserRouter } from 'react-router-dom'
import App from '../App'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { ShopDetailPage } from '../pages/ShopDetailPage'
import { BlogDetailPage } from '../pages/BlogDetailPage'
import { OrdersPage } from '../pages/OrdersPage'
import { ShopListPage } from '../pages/ShopListPage'
import { AdminLayout } from '../pages/admin/AdminLayout'
import { AdminUsersPage } from '../pages/admin/AdminUsersPage'
import { AdminBlogsPage } from '../pages/admin/AdminBlogsPage'
import { AdminShopsPage } from '../pages/admin/AdminShopsPage'
import { AdminVouchersPage } from '../pages/admin/AdminVouchersPage'
import { AdminLogsPage } from '../pages/admin/AdminLogsPage'
import { AdminRedeemPage } from '../pages/admin/AdminRedeemPage'
import { AdminSeckillDlqPage } from '../pages/admin/AdminSeckillDlqPage'

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/',
    element: <App />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'shops',
        element: <ShopListPage />,
      },
      {
        path: 'shop/:id',
        element: <ShopDetailPage />,
      },
      {
        path: 'blog/:id',
        element: <BlogDetailPage />,
      },
      {
        path: 'orders',
        element: <OrdersPage />,
      },
      {
        path: 'admin',
        element: <AdminLayout />,
        children: [
          {
            index: true,
            element: <Navigate to="users" replace />,
          },
          {
            path: 'users',
            element: <AdminUsersPage />,
          },
          {
            path: 'blogs',
            element: <AdminBlogsPage />,
          },
          {
            path: 'shops',
            element: <AdminShopsPage />,
          },
          {
            path: 'vouchers',
            element: <AdminVouchersPage />,
          },
          {
            path: 'redeem',
            element: <AdminRedeemPage />,
          },
          {
            path: 'seckill-dlq',
            element: <AdminSeckillDlqPage />,
          },
          {
            path: 'logs',
            element: <AdminLogsPage />,
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
])
