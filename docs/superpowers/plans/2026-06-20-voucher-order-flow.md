# Voucher Order Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a demo-friendly voucher purchase flow and a user-facing order list page.

**Architecture:** Extend the existing voucher-order backend with normal voucher purchase and current-user order listing. Reuse the existing seckill endpoint for seckill vouchers, and add frontend UI on shop detail plus a `/orders` page for status visibility.

**Tech Stack:** Spring Boot, MyBatis-Plus, MySQL, Redis/MQ seckill flow, React Router, React Query-style service modules.

---

### Task 1: Backend Order API

**Files:**
- Modify: `src/main/java/com/hmdp/controller/VoucherOrderController.java`
- Modify: `src/main/java/com/hmdp/service/IVoucherOrderService.java`
- Modify: `src/main/java/com/hmdp/service/impl/VoucherOrderServiceImpl.java`
- Test: `src/test/java/com/hmdp/service/impl/VoucherOrderServiceImplTest.java`

- [ ] Add `POST /voucher-order/{voucherId}` for normal vouchers.
- [ ] Add `GET /voucher-order/my` to list current-user orders.
- [ ] Return order fields plus voucher title and shop name for display.

### Task 2: Frontend Purchase Entry

**Files:**
- Modify: `frontend/src/services/types.ts`
- Modify: `frontend/src/services/modules/voucher.ts`
- Modify: `frontend/src/pages/ShopDetailPage.tsx`

- [ ] Add normal voucher purchase service.
- [ ] Show buy button for normal vouchers and seckill button for seckill vouchers.
- [ ] Display queued/success/error message after user action.

### Task 3: My Orders Page

**Files:**
- Create: `frontend/src/pages/OrdersPage.tsx`
- Modify: `frontend/src/app/router.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/styles/index.css`

- [ ] Add `/orders` route.
- [ ] Add topbar navigation entry.
- [ ] Render order status, voucher title, shop name, and timestamps.

### Task 4: Validation

**Files:**
- Verify backend and frontend build.

- [ ] Run focused backend tests.
- [ ] Run `mvn -DskipTests package`.
- [ ] Run `npm --prefix frontend run build`.
