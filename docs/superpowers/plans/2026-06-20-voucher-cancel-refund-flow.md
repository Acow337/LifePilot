# Voucher Cancel Refund Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend voucher orders with cancel and refund lifecycle states so the order system covers unpaid cancellation and paid refund review.

**Architecture:** Add backend state transition APIs for cancel, refund request, refund approval, refund rejection, and timeout cancellation scanning. Expose user actions in `/orders` and admin refund review in a new后台 page.

**Tech Stack:** Spring Boot, MyBatis-Plus, MySQL, React, TypeScript, React Router.

---

### Task 1: Backend Cancel and Refund State Machine

**Files:**
- Modify: `src/main/java/com/hmdp/controller/VoucherOrderController.java`
- Modify: `src/main/java/com/hmdp/service/IVoucherOrderService.java`
- Modify: `src/main/java/com/hmdp/service/impl/VoucherOrderServiceImpl.java`
- Test: `src/test/java/com/hmdp/service/impl/VoucherOrderServiceImplTest.java`

- [ ] Add user cancel API for unpaid orders.
- [ ] Add refund request API for paid orders.
- [ ] Add admin refund approve/reject APIs.
- [ ] Add timeout cancellation API for unpaid orders older than 15 minutes.

### Task 2: User Order Page Actions

**Files:**
- Modify: `frontend/src/pages/OrdersPage.tsx`
- Modify: `frontend/src/services/modules/voucher.ts`
- Modify: `frontend/src/services/types.ts`

- [ ] Show cancel button for unpaid orders.
- [ ] Show refund request button for paid orders.
- [ ] Refresh order list after actions.

### Task 3: Admin Refund Review Page

**Files:**
- Create: `frontend/src/pages/admin/AdminRefundsPage.tsx`
- Modify: `frontend/src/app/router.tsx`
- Modify: `frontend/src/pages/admin/AdminLayout.tsx`
- Modify: `frontend/src/styles/index.css`

- [ ] Add `/admin/refunds` page.
- [ ] List current orders and highlight refunding orders.
- [ ] Add approve/reject buttons.

### Task 4: Validation

**Files:**
- Verify backend tests and frontend build.

- [ ] Run focused backend tests.
- [ ] Run backend package build.
- [ ] Run frontend production build.
