"""Portable regression checks for the SQL server-contract and migration guard."""

from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "docs/sql/musicflow_community_moderation_guard_v14_1.sql"
DELETE_SQL = ROOT / "docs/sql/musicflow_community_delete_report_cleanup_v14_2.sql"


class CommunityModerationSqlGuardTest(unittest.TestCase):
    def test_dismiss_report_is_not_restore_comment(self):
        sql = SQL.read_text(encoding="utf-8")
        self.assertIn("if p_hide then", sql)
        self.assertIn("set status='hidden'", sql)
        self.assertNotIn("else 'visible'", sql)
        self.assertNotIn("else status='visible'", sql)
        self.assertIn("where comment_id=p_comment and status='pending'", sql)

    def test_privileged_action_requires_role_and_pending_report(self):
        sql = SQL.read_text(encoding="utf-8")
        self.assertIn("uid is null or not public.musicflow_community_can_moderate()", sql)
        self.assertIn("r.status='pending'", sql)
        self.assertIn("c.author_id<>uid", sql)
        self.assertIn("insert into musicflow_private.role_audit", sql)
        self.assertIn("to authenticated;", sql)
        self.assertIn("from public, anon;", sql)

    def test_author_deletion_resolves_pending_reports_atomically(self):
        sql = DELETE_SQL.read_text(encoding="utf-8")
        self.assertIn("set status='deleted', body=''", sql)
        self.assertIn("author_id=uid and status='visible'", sql)
        self.assertIn("update public.music_comment_reports", sql)
        self.assertIn("set status='handled'", sql)
        self.assertIn("where comment_id=p_comment and status='pending'", sql)
        self.assertIn("to authenticated;", sql)


if __name__ == "__main__":
    unittest.main()
