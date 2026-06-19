# Demo Database Seed Design

## Goal

Add a repeatable demo seed dataset so frontend pages, admin screens, seckill flows, and the smart customer service all have representative data to show.

## Scope

- Add one standalone SQL seed file executed after the base schema and compatibility scripts.
- Keep the seed small and stable, prioritizing breadth over volume.
- Use a reserved ID range starting at `9001` to avoid collisions with base course data.
- Make the script repeatable by deleting records in the reserved range before insertion.

## Dataset

- Shops: multiple categories and areas with realistic names, ratings, prices, sales, and open hours.
- Blogs: public, hidden, pending, and rejected examples for frontend and admin review demos.
- Comments: top-level comments, replies, reported comments, and hidden comments.
- Vouchers: normal vouchers and seckill vouchers with active/upcoming/expired style metadata.
- Orders: paid, used, canceled, refunding, and refunded examples.
- Social: follows, user info, and sign-in rows.
- Admin: operation logs for blog review, voucher changes, user moderation, and stock adjustment.

## Stability Rules

- Do not insert secrets or personal API keys.
- Do not require external assets; use existing placeholder image paths or stable remote image URLs already used by the project.
- Do not mutate existing course sample rows outside the reserved `9000+` demo ID range.
- Keep init time fast enough for local development.
