-- FreakCode v2 launch: delete ALL user accounts and everything tied to them
-- (progress, notes, activity history, email codes, roles). The 100 questions are kept.
--
-- BEFORE running:
--   1. Neon -> Branches -> Create branch, name "backup-before-v2", parent "production",
--      Auto-delete: Never. That is your undo button.
--   2. Run this in Neon -> SQL Editor with the "production" branch selected.
--
-- CASCADE empties every table that references users, whether or not v2 has created it yet.
TRUNCATE TABLE users RESTART IDENTITY CASCADE;

-- Check: should return 0 users and 100 questions.
SELECT (SELECT COUNT(*) FROM users) AS users, (SELECT COUNT(*) FROM questions) AS questions;
