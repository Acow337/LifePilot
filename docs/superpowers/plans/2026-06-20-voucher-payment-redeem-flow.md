# Voucher Payment Redeem Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete voucher order lifecycle from unpaid order to simulated payment and merchant/admin redemption.

**Architecture:** Extend voucher-order service with idempotent state transitions. The user order page triggers simulated payment, while an admin redemption page verifies an order code and changes paid orders to used.

**Tech Stack:** Spring Boot, MyBatis-Plus, MySQL, React, TypeScript, React Router.

---

### Task 1: Backend Payment and Redemption API

**Files:**
- Modify: `src/main/java/com/hmdp/controller/VoucherOrderController.java`
- Modify: `src/main/java/com/hmdp/service/IVoucherOrderService.java`
- Modify: `src/main/java/com/hmdp/service/impl/VoucherOrderServiceImpl.java`
- Test: `src/test/java/com/hmdp/service/impl/VoucherOrderServiceImplTest.java`

- [ ] Add `POST /voucher-order/{orderId}/pay` for current users to simulate payment.
- [ ] Add `POST /voucher-order/{orderId}/redeem` for admin/merchant redemption.
- [ ] Enforce valid transitions: unpaid → paid → used, and reject duplicate or invalid transitions.

### Task 2: Frontend User Order Actions

**Files:**
- Modify: `frontend/src/pages/OrdersPage.tsx`
- Modify: `frontend/src/services/modules/voucher.ts`
- Modify: `frontend/src/services/types.ts`

- [ ] Add API calls for pay and redeem.
- [ ] Show pay button for unpaid orders.
- [ ] Show redemption code for paid orders.

### Task 3: Admin Redemption Page

**Files:**
- Create: `frontend/src/pages/admin/AdminRedeemPage.tsx`
- Modify: `frontend/src/app/router.tsx`
- Modify: `frontend/src/pages/admin/AdminLayout.tsx`
- Modify: `frontend/src/styles/index.css`

- [ ] Add `/admin/redeem` page with order-code input.
- [ ] Call redemption API and show result.
- [ ] Add admin nav entry.

### Task 4: Validation

**Files:**
- Verify backend tests and frontend build.

- [ ] Run focused backend tests.
- [ ] Run backend package build.
- [ ] Run frontend production build.
