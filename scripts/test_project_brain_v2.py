"""Deterministic Brain v2 safety tests. No Android emulator or network required."""

import hashlib
import json
import subprocess
import tempfile
import unittest
from pathlib import Path

import project_brain_v2 as brain


class BrainV2Tests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        subprocess.run(
            ["git", "init", "-q", str(self.root)],
            capture_output=True, check=True
        )
        subprocess.run(
            ["git", "-C", str(self.root), "config", "user.email", "qa@example.invalid"],
            check=True, capture_output=True
        )
        subprocess.run(
            ["git", "-C", str(self.root), "config", "user.name", "Brain QA"],
            check=True, capture_output=True
        )
        (self.root / "proof.txt").write_text("BUILD_PASS\n", encoding="utf-8")
        subprocess.run(
            ["git", "-C", str(self.root), "add", "proof.txt"],
            check=True, capture_output=True
        )
        subprocess.run(
            ["git", "-C", str(self.root), "commit", "-m", "fixture", "-q"],
            check=True, capture_output=True
        )
        self.commit = brain.git(["rev-parse", "--short", "HEAD"], self.root).stdout.strip()

    def log_proof(self):
        return {
            "id": "qa-test", "kind": "local_log",
            "path": "qa/test.log", "contains": "PASS=TRUE", "scope": "QA fixture",
        }

    def test_priority_formula_is_deterministic_and_p1_requires_proof(self):
        factors = {"risk": 3, "user_value": 5, "evidence_gap": 5,
                   "urgency": 4, "effort": 3}
        self.assertEqual(30, brain.decision_score(factors))
        self.assertEqual("P2_HIGH", brain.priority_for(30))
        self.assertEqual("P2_HIGH", brain.priority_for(30, True, False))
        self.assertEqual("P1_CRITICAL", brain.priority_for(30, True, True))
        with self.assertRaises(ValueError):
            brain.decision_score({**factors, "effort": 0})

    def test_reject_path_traversal_and_absolute_path(self):
        for candidate in ("../private", "/etc/passwd", "C:/private", "a\\..\\x"):
            with self.subTest(candidate=candidate):
                with self.assertRaises(ValueError):
                    brain.safe_path(candidate, self.root)

    def test_repo_anchor_rejects_fabricated_claims(self):
        item = {
            "kind": "repo", "path": "proof.txt",
            "contains": "BUILD_PASS", "commit": self.commit
        }
        self.assertEqual("verified_repo", brain.verify_evidence(item, self.root).status)
        fabricated = {**item, "contains": "NON_EXISTENT_PASS"}
        self.assertEqual(
            "contradicted", brain.verify_evidence(fabricated, self.root).status
        )
        (self.root / "proof.txt").write_text("Changed\n", encoding="utf-8")
        self.assertEqual("stale", brain.verify_evidence(item, self.root).status)

    def test_ignored_log_cannot_be_called_cross_session_proof(self):
        proof = self.log_proof()
        self.assertEqual("unavailable", brain.verify_evidence(proof, self.root).status)
        folder = self.root / "qa"
        folder.mkdir()
        (folder / "test.log").write_text("FAIL", encoding="utf-8")
        self.assertEqual("contradicted", brain.verify_evidence(proof, self.root).status)
        (folder / "test.log").write_text("PASS=TRUE", encoding="utf-8")
        self.assertEqual("verified_local", brain.verify_evidence(proof, self.root).status)

    def test_powershell_utf16_log_marker_is_detected(self):
        proof = self.log_proof()
        folder = self.root / "qa"
        folder.mkdir()
        (folder / "test.log").write_bytes("PASS=TRUE\r\n".encode("utf-16"))
        self.assertEqual("verified_local", brain.verify_evidence(proof, self.root).status)

    def test_hash_integrity_for_untracked_apk(self):
        artifact = self.root / "dist" / "qa.apk"
        artifact.parent.mkdir()
        artifact.write_bytes(b"QA_APK")
        proof = {
            "kind": "artifact", "path": "dist/qa.apk",
            "sha256": hashlib.sha256(b"QA_APK").hexdigest()
        }
        self.assertEqual("verified_local", brain.verify_evidence(proof, self.root).status)
        artifact.write_bytes(b"UNTRUSTED")
        self.assertEqual("contradicted", brain.verify_evidence(proof, self.root).status)

    def fixture_brain(self):
        decisions = [
            {"id": "accessibility-v7", "title": "Review TalkBack",
             "factors": {"risk": 3, "user_value": 5, "evidence_gap": 5,
                         "urgency": 4, "effort": 3},
             "blocker": False, "priority": "P2_HIGH", "score": 30,
             "status": "pending", "evidence_refs": ["qa-test"]}
        ]
        return {
            "meta": {
                "compression_status": "MAXIMUM_DENSITY",
                "evolution_version": "v6.1-BrainV2"
            },
            "core_essence": {"priority_index": {
                "P1_CRITICAL": [], "P2_HIGH": ["Review TalkBack"],
                "P3_MEDIUM": [], "P4_LOW": [], "P5_BACKLOG": [],
            }},
            "evolved_rules": [],
            "experience_learned": {
                "success_heuristics": [], "failure_modes_prevented": []
            },
            "assumptions_to_validate": [], "entropy_pruned_nodes": [],
            "meta_cognition_and_cross_model_prompt": "",
            "brain_v2": {
                "engine_version": "2.0",
                "decision_policy": {"weights": brain.WEIGHTS},
                "baseline_commit": self.commit,
                "evidence_ledger": [self.log_proof()],
                "decisions": decisions,
                "learning_history": []
            }
        }

    def test_audit_accepts_missing_local_evidence_only_as_warning(self):
        payload = self.fixture_brain()
        report = brain.audit(payload, self.root)
        self.assertEqual([], report["errors"])
        self.assertEqual(1, report["metrics"]["unavailable_proofs"])
        self.assertTrue(any("Local evidence" in w for w in report["warnings"]))

    def test_audit_rejects_priority_divergence(self):
        payload = self.fixture_brain()
        payload["core_essence"]["priority_index"]["P2_HIGH"] = []
        report = brain.audit(payload, self.root)
        self.assertTrue(any("Priority index" in e for e in report["errors"]))
        self.assertTrue(brain.sync(payload, self.root))
        self.assertEqual(["Review TalkBack"],
                         payload["core_essence"]["priority_index"]["P2_HIGH"])
        self.assertFalse(brain.sync(payload, self.root))

    def test_learning_requires_checked_evidence_is_bounded_and_deduplicated(self):
        payload = self.fixture_brain()
        with self.assertRaises(ValueError):
            brain.review(payload, "accessibility-v7", "success", "qa-test",
                         "Verified real TalkBack", self.root)
        folder = self.root / "qa"
        folder.mkdir()
        (folder / "test.log").write_text("PASS=TRUE\n", encoding="utf-8")
        self.assertTrue(brain.review(
            payload, "accessibility-v7", "success", "qa-test",
            "Verified a specific QA scenario", self.root
        ))
        self.assertFalse(brain.review(
            payload, "accessibility-v7", "success", "qa-test",
            "Verified a specific QA scenario", self.root
        ))
        self.assertEqual(1, len(payload["brain_v2"]["learning_history"]))
        self.assertEqual("local_only",
                         payload["brain_v2"]["learning_history"][0]["scope"])
        self.assertEqual("v6.1-BrainV2-r1", payload["meta"]["evolution_version"])
        self.assertEqual(1, payload["brain_v2"]["learning_revision"])

    def test_partial_learning_preserves_unknown_for_later_sessions(self):
        payload = self.fixture_brain()
        (self.root / "qa").mkdir()
        (self.root / "qa" / "test.log").write_text("PASS=TRUE", encoding="utf-8")
        brain.review(payload, "accessibility-v7", "partial", "qa-test",
                     "Voice navigation still unknown", self.root)
        self.assertIn("Voice navigation still unknown", payload["assumptions_to_validate"])

    def test_learned_pass_with_only_document_anchor_cannot_prove_runtime(self):
        payload = self.fixture_brain()
        payload["brain_v2"]["evidence_ledger"].append({
            "id": "source-proof", "kind": "repo", "path": "proof.txt",
            "contains": "BUILD_PASS", "commit": self.commit
        })
        with self.assertRaises(ValueError):
            brain.review(payload, "accessibility-v7", "success", "source-proof",
                         "Runtime definitely passed", self.root)

    def test_committed_utf8_qa_log_can_be_portable_runtime_proof(self):
        payload = self.fixture_brain()
        qa = self.root / "qa-logs"
        qa.mkdir()
        (qa / "v7.log").write_text("LIBRARY_ACCESSIBILITY_V7_FINAL_RESULT=PASS\n",
                                    encoding="utf-8")
        subprocess.run(["git", "-C", str(self.root), "add", "qa-logs/v7.log"],
                       check=True, capture_output=True)
        subprocess.run(["git", "-C", str(self.root), "commit", "-m", "QA", "-q"],
                       check=True, capture_output=True)
        commit = brain.git(["rev-parse", "--short", "HEAD"], self.root).stdout.strip()
        payload["brain_v2"]["evidence_ledger"].append({
            "id": "qa-v7-portable", "kind": "repo", "claim_type": "test_log",
            "path": "qa-logs/v7.log",
            "contains": "LIBRARY_ACCESSIBILITY_V7_FINAL_RESULT=PASS",
            "commit": commit
        })
        self.assertTrue(brain.review(
            payload, "accessibility-v7", "success", "qa-v7-portable",
            "Committed runtime test truly passed", self.root
        ))
        self.assertEqual("repo", payload["brain_v2"]["learning_history"][-1]["scope"])

    def test_committed_utf16_powershell_qa_is_portable(self):
        qa = self.root / "qa-logs"
        qa.mkdir()
        (qa / "android.log").write_bytes(
            "LIBRARY_ACCESSIBILITY_V7_FINAL_RESULT=PASS\r\n".encode("utf-16")
        )
        subprocess.run(
            ["git", "-C", str(self.root), "add", "qa-logs/android.log"],
            check=True, capture_output=True,
        )
        subprocess.run(
            ["git", "-C", str(self.root), "commit", "-m", "QA utf16", "-q"],
            check=True, capture_output=True,
        )
        commit = brain.git(["rev-parse", "--short", "HEAD"], self.root).stdout.strip()
        result = brain.verify_evidence({
            "id": "android-utf16", "kind": "repo", "claim_type": "test_log",
            "path": "qa-logs/android.log",
            "contains": "LIBRARY_ACCESSIBILITY_V7_FINAL_RESULT=PASS",
            "commit": commit,
        }, self.root)
        self.assertEqual("verified_repo", result.status)

    def test_credential_and_memory_budget_guards(self):
        payload = self.fixture_brain()
        path = self.root / "brain.json"
        payload["evolved_rules"] = ["token=sb_secret_veryverysecret"]
        with self.assertRaises(ValueError):
            brain.write_brain(payload, path)
        payload["evolved_rules"] = ["x" * 30_000]
        with self.assertRaises(ValueError):
            brain.write_brain(payload, path)


if __name__ == "__main__":
    unittest.main()
