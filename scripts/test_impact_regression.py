"""QA architecture contracts; no Android SDK, network, media or DB needed."""

import copy
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).parent))
import impact_regression as qa


class ImpactDrivenRegressionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.conf = qa.load_config()
        cls.cases, cls.unmapped = qa.inventory(cls.conf)

    def selection(self, path: str):
        return qa.select(self.conf, self.cases, files=[path])

    def test_inventory_has_no_unmapped_or_invented_smoke(self):
        metrics = qa.validate(self.conf, self.cases, self.unmapped)
        self.assertEqual(22, metrics["modules"])
        self.assertGreaterEqual(metrics["unique_test_classes"], 105)
        self.assertGreaterEqual(metrics["unique_test_methods"], 507)
        self.assertGreater(metrics["module_case_links"], metrics["unique_test_methods"])

    def test_community_moderation_or_supabase_is_high_risk(self):
        for file in (
            "app/src/main/java/com/example/musicflow/feature/community/CommunityModerationPanel.kt",
            "app/src/main/java/com/example/musicflow/core/community/SupabaseCommunityRepository.kt",
            "docs/sql/musicflow_community_moderation_guard_v14_1.sql",
            "docs/sql/musicflow_community_delete_report_cleanup_v14_2.sql",
        ):
            result = self.selection(file)
            self.assertEqual("full_required", result["mode"])
            self.assertGreaterEqual(result["test_count"], 478)

    def test_search_change_only_search_and_downstream_home(self):
        result = self.selection(
            "app/src/main/java/com/example/musicflow/feature/search/SearchViewModel.kt"
        )
        self.assertEqual("selective", result["mode"])
        self.assertEqual(["SEARCH"], result["direct_modules"])
        self.assertEqual(["HOME_NAV"], result["affected_modules"])
        ids = {item["id"] for item in result["testcases"]}
        self.assertIn("SEARCH/AdvancedSearchIndexV8Test#refreshRevocationAndQueryChangesDoNotReuseStaleCatalog", ids)
        self.assertIn("HOME_NAV/ListeningJourneysV1UiTest#homeShowsShortHistoryThenAllSongsWithOriginalPlaybackIndex", ids)
        self.assertFalse(any(x.startswith("BILLING_DEMO/") for x in ids))
        self.assertLess(result["test_count"], 50)

    def test_demo_billing_selects_downstream_demo_center_not_playback(self):
        result = self.selection(
            "app/src/main/java/com/example/musicflow/feature/account/DemoBillingScreen.kt"
        )
        self.assertEqual("selective", result["mode"])
        self.assertEqual(["BILLING_DEMO"], result["direct_modules"])
        self.assertEqual(["DEMO_CENTER"], result["affected_modules"])
        self.assertTrue(any(i["module"] == "DEMO_CENTER" for i in result["testcases"]))
        self.assertFalse(any(i["module"] == "PLAYBACK" for i in result["testcases"]))

    def test_auth_change_propagates_roles_creator_billing_and_cloud(self):
        result = self.selection(
            "app/src/main/java/com/example/musicflow/core/account/AccountAuthErrorLabels.kt"
        )
        self.assertEqual("selective", result["mode"])
        self.assertEqual(["AUTH"], result["direct_modules"])
        for module in ["ROLES_ADMIN", "CREATOR", "BILLING_DEMO", "CLOUD_SYNC", "DEMO_CENTER"]:
            self.assertIn(module, result["affected_modules"])

    def test_oauth_token_and_admin_web_changes_require_security_full(self):
        for path in [
            "app/src/main/java/com/example/musicflow/core/account/GoogleOAuthFlow.kt",
            "app/src/main/java/com/example/musicflow/core/account/DemoPasswordHash.kt",
            "admin-web/index.html",
        ]:
            with self.subTest(path=path):
                self.assertEqual("full_required", self.selection(path)["mode"])

    def test_real_saf_grant_change_forces_full_suite(self):
        result = self.selection(
            "app/src/main/java/com/example/musicflow/core/media/LocalPickedMusicStore.kt"
        )
        self.assertEqual("full_required", result["mode"])
        self.assertEqual(["MEDIA_SAF"], result["direct_modules"])
        self.assertGreaterEqual(result["test_count"], 450)
        self.assertTrue(result["risk_reasons"])

    def test_shared_gradle_theme_navigation_and_manifest_escalate(self):
        for path in [
            "app/build.gradle.kts",
            "app/src/main/AndroidManifest.xml",
            "app/src/main/java/com/example/musicflow/navigation/MusicFlowApp.kt",
            "app/src/main/java/com/example/musicflow/ui/theme/Theme.kt",
        ]:
            with self.subTest(path=path):
                self.assertEqual("full_required", self.selection(path)["mode"])

    def test_shared_ui_component_selects_downstream_screen_contracts(self):
        result = self.selection(
            "app/src/main/java/com/example/musicflow/ui/components/OceanFlowVisuals.kt"
        )
        self.assertEqual("selective", result["mode"])
        self.assertEqual(["UI_ACCESS"], result["direct_modules"])
        for consumer in ["LIBRARY", "SEARCH", "PLAYBACK", "HOME_NAV", "AUTH"]:
            self.assertIn(consumer, result["affected_modules"])
        self.assertTrue(any(i["module"] == "LIBRARY" for i in result["testcases"]))
        self.assertLess(result["test_count"], 457)

    def test_owner_rbac_mutation_forces_full_security_regression(self):
        result = self.selection(
            "app/src/main/java/com/example/musicflow/core/account/MusicFlowAccess.kt"
        )
        self.assertEqual("full_required", result["mode"])
        self.assertIn("ROLES_ADMIN", result["direct_modules"])

    def test_unknown_source_does_not_silently_skip(self):
        result = self.selection(
            "app/src/main/java/com/example/musicflow/feature/newfeature/Unmapped.kt"
        )
        self.assertEqual("full_required", result["mode"])
        self.assertEqual(1, len(result["unclassified"]))

    def test_docs_only_requires_brain_and_selector_validation_not_android(self):
        result = self.selection("docs/Cross_Session_Project_Brain.json")
        self.assertEqual("qa_docs_only", result["mode"])
        self.assertFalse(result["testcases"])
        self.assertTrue(any("test_impact_regression.py" in c for c in qa.commands(result)))

    def test_touching_test_file_selects_its_module(self):
        result = self.selection(
            "app/src/androidTest/java/com/example/musicflow/feature/library/LibraryKeyboardAccessibilityV8Test.kt"
        )
        self.assertEqual("selective", result["mode"])
        self.assertEqual(["UI_ACCESS"], result["direct_modules"])

    def test_duplicate_module_cases_are_only_selected_once(self):
        result = qa.select(self.conf, self.cases, modules=["SEARCH", "PLAYBACK"])
        keys = [(case["class"], case["method"]) for case in result["testcases"]]
        self.assertEqual(len(keys), len(set(keys)))

    def test_module_paths_do_not_allow_traversal_or_absolute(self):
        for path in ["../secret", "/etc/passwd", "C:\\private\\token.txt"]:
            with self.subTest(path=path), self.assertRaises(ValueError):
                self.selection(path)

    def test_validation_rejects_stale_smoke_and_dependency_cycles(self):
        broken = copy.deepcopy(self.conf)
        broken["modules"][0]["smoke"] = ["MadeUpTest#noSuchMethod"]
        with self.assertRaisesRegex(ValueError, "Missing/stale smoke"):
            qa.validate(broken, self.cases, self.unmapped)
        cyclic = copy.deepcopy(self.conf)
        cyclic["modules"][0]["requires"] = ["LIBRARY"]
        with self.assertRaisesRegex(ValueError, "Circular"):
            qa.validate(cyclic, self.cases, self.unmapped)

    def test_generated_catalog_contains_real_cases_from_each_module(self):
        text = qa.catalog(self.conf, self.cases)
        for module in self.conf["modules"]:
            self.assertIn("### " + module["id"], text)
            self.assertIn(self.cases[module["id"]][0].id, text)
        self.assertIn("LibraryKeyboardAccessibilityV8Test#keyboardEnter", text)

    def test_release_packaging_is_full_gate(self):
        result = self.selection("scripts/build-musicflow-demo-apk-v1.ps1")
        self.assertEqual("full_required", result["mode"])
        self.assertIn("RELEASE_BACKUP", result["direct_modules"])
        command_list = qa.commands(result)
        self.assertTrue(any("run-signed-upgrade-v14-qa.ps1" in c for c in command_list))
        self.assertFalse(any("am instrument" in c and "SignedApkUpgradeV6Test" in c
                             for c in command_list))

    def test_online_rights_extra_test_uses_repo_root_compatible_invocation(self):
        module = next(m for m in self.conf["modules"] if m["id"] == "ONLINE_RIGHTS")
        self.assertIn("py -3 scripts/test_musicflow_licensed_upload.py", module["extra"])
        self.assertNotIn("py -3 -m unittest scripts/test_musicflow_licensed_upload.py",
                         module["extra"])


if __name__ == "__main__":
    unittest.main()
