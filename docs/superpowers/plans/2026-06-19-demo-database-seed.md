# Demo Database Seed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add repeatable demo seed data covering frontend, admin, seckill, social, and smart customer-service scenarios.

**Architecture:** Create a standalone SQL file using reserved IDs `9001+`, delete that ID range first, then insert curated rows. Wire the file into `scripts/init-db.sh` after base schema, admin compatibility, and performance indexes.

**Tech Stack:** MySQL SQL seed files, Bash init script, Maven validation.

---

### Task 1: Create Demo Seed SQL

**Files:**
- Create: `src/main/resources/db/hmdp_demo_seed.sql`

- [ ] Add repeatable cleanup statements for `tb_voucher_order`, `tb_seckill_voucher`, `tb_voucher`, `tb_blog_comments`, `tb_blog`, `tb_follow`, `tb_sign`, `tb_user_info`, `tb_admin_log`, `tb_shop`, and `tb_user` using IDs `>= 9000`.
- [ ] Insert demo users, shops, blogs, comments, follows, sign records, vouchers, seckill metadata, voucher orders, and admin logs.
- [ ] Keep all rows deterministic and avoid external secrets.

### Task 2: Wire Init Script

**Files:**
- Modify: `scripts/init-db.sh`

- [ ] Execute `src/main/resources/db/hmdp_demo_seed.sql` after `hmdp_perf_indexes.sql`.
- [ ] Keep existing environment variable behavior unchanged.

### Task 3: Validate

**Files:**
- Verify: `src/main/resources/db/hmdp_demo_seed.sql`
- Verify: `scripts/init-db.sh`

- [ ] Run `bash -n scripts/init-db.sh`.
- [ ] Run `mvn -DskipTests package`.
- [ ] If MySQL is available, run `./scripts/init-db.sh` and query row counts for demo IDs.
