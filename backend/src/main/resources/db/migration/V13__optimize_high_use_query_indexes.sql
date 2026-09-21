-- ============================================================
-- M27 - Database Performance
--
-- Align indexes with the ordering used by high-traffic group
-- history queries.
--
-- PostgreSQL can scan indexes in either direction, but the
-- explicit descending order documents and directly supports
-- the application's newest-first access pattern.
-- ============================================================


-- Shared-expense history query:
--
-- WHERE group_id = ?
-- ORDER BY expense_date DESC, created_at DESC
--
-- Replace the existing two-column index with one that also
-- covers the deterministic secondary ordering column.
DROP INDEX IF EXISTS idx_shared_expenses_group_date;

CREATE INDEX idx_shared_expenses_group_date_created
    ON shared_expenses (
                        group_id,
                        expense_date DESC,
                        created_at DESC
        );


-- Settlement history query:
--
-- WHERE group_id = ?
-- ORDER BY settled_at DESC
--
-- The composite index covers both group filtering and
-- newest-first settlement ordering. It supersedes the
-- previous group_id-only index.
DROP INDEX IF EXISTS idx_settlements_group_id;

CREATE INDEX idx_settlements_group_settled_at
    ON settlements (
                    group_id,
                    settled_at DESC
        );