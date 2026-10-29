"""Offline regression checks for MusicFlow's Android account backup exclusions."""

from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
XML_DIR = ROOT / "app" / "src" / "main" / "res" / "xml"
SECRET_PREF = "musicflow_account_session_v1.xml"


class AccountBackupRulesTests(unittest.TestCase):
    def test_manifest_declares_both_backup_policies(self):
        manifest = (ROOT / "app" / "src" / "main" / "AndroidManifest.xml").read_text(
            encoding="utf-8"
        )
        self.assertIn('android:fullBackupContent="@xml/backup_rules"', manifest)
        self.assertIn('android:dataExtractionRules="@xml/data_extraction_rules"', manifest)

    def test_legacy_android_backup_excludes_account_session(self):
        root = ET.parse(XML_DIR / "backup_rules.xml").getroot()
        self.assertEqual(root.tag, "full-backup-content")
        self._assert_excluded(root)

    def test_android_12_plus_cloud_and_device_transfer_exclude_account_session(self):
        root = ET.parse(XML_DIR / "data_extraction_rules.xml").getroot()
        self.assertEqual(root.tag, "data-extraction-rules")
        for transfer in ("cloud-backup", "device-transfer"):
            with self.subTest(transfer=transfer):
                section = root.find(transfer)
                self.assertIsNotNone(section, f"Missing {transfer} rules")
                self._assert_excluded(section)

    def test_backup_exclusion_matches_account_token_store(self):
        account_api = (
            ROOT
            / "app"
            / "src"
            / "main"
            / "java"
            / "com"
            / "example"
            / "musicflow"
            / "core"
            / "account"
            / "AccountApi.kt"
        ).read_text(encoding="utf-8")
        self.assertIn('"musicflow_account_session_v1"', account_api)
        self.assertIn('"google_pending"', account_api)
        self.assertIn('"refresh"', account_api)

    def _assert_excluded(self, parent):
        excluded = {
            (node.get("domain"), node.get("path")) for node in parent.findall("exclude")
        }
        self.assertIn(("sharedpref", SECRET_PREF), excluded)
        self.assertFalse(parent.findall("include"), "Avoid include rules that override defaults")


if __name__ == "__main__":
    unittest.main(verbosity=2)
