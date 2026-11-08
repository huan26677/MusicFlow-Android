#!/usr/bin/env python3
"""Evidence-driven, cross-session MusicFlow brain. Python stdlib only.

Commands:
    python scripts/project_brain_v2.py check
    python scripts/project_brain_v2.py sync
    python scripts/project_brain_v2.py review --decision ID --outcome success \
        --evidence EVIDENCE_ID --lesson "Short, verified conclusion"

The canonical memory is always docs/Cross_Session_Project_Brain.json.
No command runs builds, touches user media or silently changes git.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
BRAIN_PATH = ROOT / "docs" / "Cross_Session_Project_Brain.json"
PRIORITIES = ("P1_CRITICAL", "P2_HIGH", "P3_MEDIUM", "P4_LOW", "P5_BACKLOG")
REQUIRED = (
    "meta", "core_essence", "evolved_rules", "experience_learned",
    "assumptions_to_validate", "entropy_pruned_nodes",
    "meta_cognition_and_cross_model_prompt", "brain_v2"
)
WEIGHTS = {"risk": 3, "user_value": 2, "evidence_gap": 2, "urgency": 1, "effort": -1}
MAX_BYTES = 24_000
MAX_LEARNINGS = 8
SECRET_PATTERNS = (
    re.compile(r"sb_secret_[A-Za-z0-9_-]+"),
    re.compile(r"ghp_[A-Za-z0-9]{20,}"),
    re.compile(r"sk-[A-Za-z0-9_-]{20,}"),
    re.compile(r"(?i)(?:service_role_key|authorization\s*:\s*bearer)\s*[:=]\s*\S+"),
)


@dataclass(frozen=True)
class Proof:
    status: str
    explanation: str


def safe_path(relative: str, root: Path = ROOT) -> Path:
    """Reject absolute, traversal, symlink escapes, and Windows drive names."""
    if not isinstance(relative, str) or not relative or "\\" in relative:
        raise ValueError("Evidence path must be a repository-relative POSIX path")
    pure = PurePosixPath(relative)
    if pure.is_absolute() or ".." in pure.parts or ":" in pure.parts[0]:
        raise ValueError("Evidence path is outside the repository")
    path = root.joinpath(*pure.parts).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError("Evidence path escapes the repository")
    return path


def git(args: list[str], root: Path = ROOT) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", "-C", str(root), *args],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
        check=False
    )


def decision_score(factors: dict[str, int]) -> int:
    if set(factors) != set(WEIGHTS):
        raise ValueError("Decision factors must match the five policy weights")
    if any(type(value) is not int or not 1 <= value <= 5 for value in factors.values()):
        raise ValueError("Decision factors must be integers between 1 and 5")
    return sum(WEIGHTS[name] * factors[name] for name in WEIGHTS)


def priority_for(score: int, blocker: bool = False, blocker_verified: bool = False) -> str:
    if blocker and blocker_verified and score >= 24:
        return "P1_CRITICAL"
    if score >= 24:
        return "P2_HIGH"
    if score >= 16:
        return "P3_MEDIUM"
    if score >= 10:
        return "P4_LOW"
    return "P5_BACKLOG"


def load_brain(path: Path = BRAIN_PATH) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write_brain(brain: dict[str, Any], path: Path = BRAIN_PATH) -> None:
    # A compact single-line JSON record retains every evidence item within
    # the 24 KB memory limit. Use a JSON formatter when human review is needed.
    serialized = json.dumps(
        brain, ensure_ascii=False, separators=(",", ":")
    ) + "\n"
    if len(serialized.encode("utf-8")) > MAX_BYTES:
        raise ValueError("Brain exceeded its 24 KB memory budget; compress first")
    if any(pattern.search(serialized) for pattern in SECRET_PATTERNS):
        raise ValueError("Possible credential in project brain: refuse to write")
    path.write_text(serialized, encoding="utf-8", newline="\n")


def decode_evidence_bytes(data: bytes) -> str:
    """Windows PowerShell 5.1 redirected QA logs are UTF-16, not UTF-8."""
    if data.startswith((b"\xff\xfe", b"\xfe\xff")):
        return data.decode("utf-16")
    if data.startswith(b"\xef\xbb\xbf"):
        return data.decode("utf-8-sig")
    if b"\x00" in data[:256]:
        # Old QA exports sometimes write UTF-16 without a BOM.
        try:
            return data.decode("utf-16-le")
        except UnicodeDecodeError:
            pass
    return data.decode("utf-8", errors="replace")


def read_evidence_text(path: Path) -> str:
    return decode_evidence_bytes(path.read_bytes())


def verify_evidence(item: dict[str, Any], root: Path = ROOT) -> Proof:
    kind = item.get("kind")
    location = item.get("path")
    try:
        path = safe_path(location, root)
    except (ValueError, TypeError) as exc:
        return Proof("contradicted", str(exc))
    if kind not in {"repo", "local_log", "artifact"}:
        return Proof("contradicted", "Unsupported evidence kind")
    if kind == "repo":
        commit = item.get("commit", "")
        if not isinstance(commit, str) or not re.fullmatch(r"[a-f0-9]{7,40}", commit):
            return Proof("contradicted", "No valid Git commit anchor")
        # Git show preserves the original log encoding. A UTF-16 PowerShell
        # QA log has to be decoded BEFORE checking its PASS marker.
        show = subprocess.run(
            ["git", "-C", str(root), "show", f"{commit}:{location}"],
            capture_output=True, check=False,
        )
        if show.returncode:
            return Proof("contradicted", "Committed source is missing at this Git revision")
        marker = item.get("contains", "")
        if not isinstance(marker, str) or not marker or marker not in decode_evidence_bytes(show.stdout):
            return Proof("contradicted", "Claim marker is absent at the Git anchor")
        if not path.is_file() or marker not in read_evidence_text(path):
            return Proof("stale", "Original commit verified, current file changed")
        return Proof("verified_repo", f"Anchored in Git {commit}")
    if not path.is_file():
        return Proof("unavailable", "Local evidence missing; cannot re-assert PASS here")
    if kind == "artifact":
        expected = item.get("sha256", "")
        if not isinstance(expected, str) or not re.fullmatch(r"[a-fA-F0-9]{64}", expected):
            return Proof("contradicted", "Malformed SHA-256")
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual.lower() != expected.lower():
            return Proof("contradicted", "APK hash mismatch")
        return Proof("verified_local", "SHA-256 matches local APK (not tracked by Git)")
    marker = item.get("contains", "")
    if not isinstance(marker, str) or not marker:
        return Proof("contradicted", "Local log has no required PASS marker")
    if marker not in read_evidence_text(path):
        return Proof("contradicted", "Required QA marker absent: do NOT claim PASS")
    return Proof("verified_local", "Marker found in local ignored QA log")


def expected_priorities(
    decisions: list[dict[str, Any]],
    statuses: dict[str, Proof],
) -> tuple[dict[str, list[str]], list[str]]:
    priorities: dict[str, list[str]] = {name: [] for name in PRIORITIES}
    errors: list[str] = []
    seen: set[str] = set()
    for task in decisions:
        ident = task.get("id")
        if not isinstance(ident, str) or not re.fullmatch(r"[a-z][a-z0-9-]{2,47}", ident):
            errors.append(f"Invalid decision ID: {ident!r}")
            continue
        if ident in seen:
            errors.append(f"Repeated decision ID: {ident}")
            continue
        seen.add(ident)
        try:
            score = decision_score(task["factors"])
        except (ValueError, KeyError, TypeError) as exc:
            errors.append(f"{ident}: {exc}")
            continue
        if not isinstance(task.get("title"), str) or not task["title"].strip():
            errors.append(f"{ident}: missing actionable title")
            continue
        refs = task.get("evidence_refs", [])
        if not isinstance(refs, list) or not all(isinstance(x, str) for x in refs):
            errors.append(f"{ident}: invalid evidence refs")
            continue
        verified = bool(refs) and any(
            statuses.get(ref, Proof("unavailable", "")).status
            in {"verified_repo", "verified_local"} for ref in refs
        )
        if task.get("blocker") and not verified:
            errors.append(f"{ident}: P1/blocker requires independently checked evidence")
        priority = priority_for(score, bool(task.get("blocker")), verified)
        if task.get("score") != score or task.get("priority") != priority:
            errors.append(
                f"{ident}: score/priority drift: expected {score}/{priority}"
            )
        if task.get("status") == "done":
            continue
        priorities[priority].append(task["title"])
    return priorities, errors


def _normalized(text: str) -> str:
    return " ".join(text.lower().split())


def audit(brain: dict[str, Any], root: Path = ROOT) -> dict[str, Any]:
    errors: list[str] = []
    warnings: list[str] = []
    if not isinstance(brain, dict):
        return {"errors": ["Brain must be a JSON object"], "warnings": [], "proofs": {}}
    for key in REQUIRED:
        if key not in brain:
            errors.append(f"Missing canonical field: {key}")
    if errors:
        return {"errors": errors, "warnings": warnings, "proofs": {}}
    engine = brain["brain_v2"]
    if engine.get("engine_version") != "2.0":
        errors.append("Unexpected Brain v2 engine version")
    if engine.get("decision_policy", {}).get("weights") != WEIGHTS:
        errors.append("Scoring rules do not match deterministic engine")
    if brain["meta"].get("compression_status") != "MAXIMUM_DENSITY":
        errors.append("Memory compression contract changed")
    raw = json.dumps(brain, ensure_ascii=False).encode("utf-8")
    serialized_size = len((json.dumps(
        brain, ensure_ascii=False, separators=(",", ":")
    ) + "\n").encode("utf-8"))
    if serialized_size > MAX_BYTES:
        errors.append("Memory budget exceeded: 24 KB")
    if any(pattern.search(raw.decode("utf-8")) for pattern in SECRET_PATTERNS):
        errors.append("Possible credential detected in memory")
    proofs: dict[str, Proof] = {}
    ids: set[str] = set()
    for item in engine.get("evidence_ledger", []):
        ident = item.get("id")
        if not isinstance(ident, str) or not re.fullmatch(r"[a-z][a-z0-9-]{2,47}", ident):
            errors.append(f"Invalid evidence ID: {ident!r}")
            continue
        if ident in ids:
            errors.append(f"Duplicate evidence ID: {ident}")
            continue
        ids.add(ident)
        result = verify_evidence(item, root)
        proofs[ident] = result
        if result.status == "contradicted":
            errors.append(f"Evidence {ident} contradicted: {result.explanation}")
        elif result.status in {"unavailable", "stale"}:
            warnings.append(f"Evidence {ident}: {result.explanation}")
    tasks, decision_errors = expected_priorities(
        engine.get("decisions", []), proofs
    )
    errors.extend(decision_errors)
    priority_index = brain.get("core_essence", {}).get("priority_index", {})
    if set(priority_index) != set(PRIORITIES) or tasks != priority_index:
        errors.append("Priority index and scored decisions disagree; run sync")
    for item in engine.get("decisions", []):
        for ref in item.get("evidence_refs", []):
            if ref not in proofs:
                errors.append(f"{item.get('id')}: unknown evidence ref {ref}")
    for key in ("success_heuristics", "failure_modes_prevented"):
        items = brain.get("experience_learned", {}).get(key, [])
        if not isinstance(items, list):
            errors.append(f"{key} must be a list")
            continue
        if len(items) != len(set(map(_normalized, items))):
            warnings.append(f"Repeated memory nodes in {key}; run sync")
    for item in engine.get("learning_history", []):
        if not isinstance(item, dict) or item.get("evidence_ref") not in proofs:
            errors.append("Learning entry without a known evidence reference")
    if len(engine.get("learning_history", [])) > MAX_LEARNINGS:
        errors.append("Learning history exceeds bounded density budget")
    expected_commit = engine.get("baseline_commit")
    current = git(["rev-parse", "--short", "HEAD"], root)
    if current.returncode:
        warnings.append("Git unavailable; HEAD not independently checked")
    elif expected_commit != current.stdout.strip():
        # Updating only the brain/tooling should not make the source baseline
        # instantly obsolete. New Android/backend code DOES need fresh review.
        changed = git(
            ["diff", "--name-only", str(expected_commit), "HEAD", "--",
             "app/src", "app/build.gradle.kts", "admin-web"], root
        )
        if changed.returncode:
            warnings.append("Cannot verify whether the source baseline is still current")
        elif changed.stdout.strip():
            warnings.append(
                f"Source changed since Brain baseline {expected_commit}; "
                "review affected claims: " +
                ", ".join(changed.stdout.splitlines()[:6])
            )
    status = git(["status", "--porcelain"], root)
    if status.returncode:
        warnings.append("Cannot read working tree status")
    elif status.stdout.strip():
        warnings.append("Working tree has uncommitted changes; do not claim pushed")
    return {
        "errors": errors, "warnings": warnings,
        "proofs": {key: {"status": value.status, "detail": value.explanation}
                   for key, value in proofs.items()},
        "metrics": {
            "portable_repo_proofs": sum(p.status == "verified_repo" for p in proofs.values()),
            "local_only_proofs": sum(p.status == "verified_local" for p in proofs.values()),
            "unavailable_proofs": sum(p.status == "unavailable" for p in proofs.values()),
            "contradicted_proofs": sum(p.status == "contradicted" for p in proofs.values()),
            "priority_items": sum(map(len, tasks.values())),
        }
    }


def _unique(items: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for item in items:
        key = _normalized(item)
        if key not in seen:
            result.append(item)
            seen.add(key)
    return result


def sync(brain: dict[str, Any], root: Path = ROOT) -> bool:
    # Correct only deterministic, non-interpretive fields. NEVER invent PASS or
    # silently remove unique user lessons.
    before = json.dumps(brain, ensure_ascii=False, sort_keys=True)
    proofs = {
        p["id"]: verify_evidence(p, root)
        for p in brain["brain_v2"]["evidence_ledger"]
    }
    for task in brain["brain_v2"]["decisions"]:
        task["score"] = decision_score(task["factors"])
        refs = task.get("evidence_refs", [])
        verified = any(
            proofs.get(ref, Proof("unavailable", "")).status
            in {"verified_repo", "verified_local"} for ref in refs
        )
        task["priority"] = priority_for(
            task["score"], bool(task.get("blocker")), verified
        )
    priority_index, errors = expected_priorities(brain["brain_v2"]["decisions"], proofs)
    if errors:
        raise ValueError("; ".join(errors))
    brain["core_essence"]["priority_index"] = priority_index
    for key in ("success_heuristics", "failure_modes_prevented"):
        previous = brain["experience_learned"][key]
        brain["experience_learned"][key] = _unique(previous)
    return json.dumps(brain, ensure_ascii=False, sort_keys=True) != before


def review(
    brain: dict[str, Any], decision_id: str, outcome: str,
    evidence_id: str, lesson: str, root: Path = ROOT
) -> bool:
    if outcome not in {"success", "failure", "partial"}:
        raise ValueError("Outcome must be success, failure, or partial")
    if not lesson or len(lesson) > 250:
        raise ValueError("Lesson must be 1..250 characters")
    if any(pattern.search(lesson) for pattern in SECRET_PATTERNS):
        raise ValueError("Possible credential in lesson")
    decisions = {d["id"]: d for d in brain["brain_v2"]["decisions"]}
    if decision_id not in decisions:
        raise ValueError("Unknown task; add it to the evidence-scored decision index first")
    evidence = next(
        (p for p in brain["brain_v2"]["evidence_ledger"] if p["id"] == evidence_id),
        None
    )
    if not evidence:
        raise ValueError("Unknown evidence reference")
    proof = verify_evidence(evidence, root)
    if proof.status not in {"verified_repo", "verified_local"}:
        raise ValueError("Evidence is not independently available and verified")
    # A checked source proves that code exists, not that the runtime passed.
    # A QA log committed under qa-logs/ with its PASS marker is independently
    # re-checkable across AI sessions, unlike a documentation assertion.
    if outcome == "success" and evidence["kind"] == "repo":
        claim_type = evidence.get("claim_type")
        if claim_type == "test_log":
            if not (
                evidence.get("path", "").startswith("qa-logs/")
                and evidence["path"].endswith(".log")
                and "PASS" in evidence.get("contains", "")
            ):
                raise ValueError("Portable QA evidence must be a committed PASS log")
        elif claim_type != "source":
            raise ValueError("Use a QA log for runtime PASS; docs alone are not test proof")
    history = brain["brain_v2"]["learning_history"]
    dedup_key = hashlib.sha256(
        f"{decision_id}|{outcome}|{evidence_id}|{lesson}".encode("utf-8")
    ).hexdigest()[:12]
    if any(item["id"] == dedup_key for item in history):
        return False
    history.append({
        "id": dedup_key, "task": decision_id, "outcome": outcome,
        "evidence_ref": evidence_id, "scope": (
            "repo" if proof.status == "verified_repo" else "local_only"
        ), "lesson": lesson
    })
    del history[:-MAX_LEARNINGS]
    if outcome == "success":
        brain["experience_learned"]["success_heuristics"] = _unique(
            brain["experience_learned"]["success_heuristics"] + [lesson]
        )
    elif outcome == "failure":
        brain["experience_learned"]["failure_modes_prevented"] = _unique(
            brain["experience_learned"]["failure_modes_prevented"] + [lesson]
        )
    else:
        # Partial evidence must remain an open assumption after old events
        # are evicted from the bounded history.
        brain["assumptions_to_validate"] = _unique(
            brain["assumptions_to_validate"] + [lesson]
        )
    revision = int(brain["brain_v2"].get("learning_revision", 0)) + 1
    brain["brain_v2"]["learning_revision"] = revision
    stable_version = re.sub(r"-r\d+$", "", brain["meta"]["evolution_version"])
    brain["meta"]["evolution_version"] = f"{stable_version}-r{revision}"
    return True


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    check = sub.add_parser("check", help="Read-only integrity/evidence/priority audit")
    check.add_argument("--json", action="store_true", help="Machine-readable report")
    check.add_argument("--strict", action="store_true", help="Treat warnings as failure")
    sub.add_parser("sync", help="Write recalculated priorities and deduplicate exact memories")
    record = sub.add_parser("review", help="Record ONE verified learning outcome")
    record.add_argument("--decision", required=True)
    record.add_argument("--outcome", required=True, choices=["success", "failure", "partial"])
    record.add_argument("--evidence", required=True)
    record.add_argument("--lesson", required=True)
    args = parser.parse_args(argv)
    try:
        brain = load_brain()
        if args.command == "sync":
            updated = sync(brain)
            result = audit(brain)
            # Fresh changes can produce warnings but cannot contradict sources.
            if result["errors"]:
                raise ValueError("; ".join(result["errors"]))
            if updated:
                write_brain(brain)
            print("BRAIN_V2_SYNC=UPDATED" if updated else "BRAIN_V2_SYNC=UNCHANGED")
            return 0
        if args.command == "review":
            new = review(brain, args.decision, args.outcome, args.evidence, args.lesson)
            if new:
                sync(brain)
                write_brain(brain)
            print("BRAIN_V2_LEARNING=RECORDED" if new else "BRAIN_V2_LEARNING=DUPLICATE")
            return 0
        result = audit(brain)
        if args.json:
            print(json.dumps(result, ensure_ascii=False, indent=2))
        else:
            print("BRAIN_V2_AUDIT=" + ("FAIL" if result["errors"] else "PASS"))
            print("EVIDENCE_METRICS=" + json.dumps(result.get("metrics", {})))
            for ident, proof in result["proofs"].items():
                print(f"  {ident}: {proof['status']} ({proof['detail']})")
            for issue in result["errors"]:
                print("ERROR: " + issue)
            for issue in result["warnings"]:
                print("WARN: " + issue)
        if result["errors"]:
            return 2
        if result["warnings"] and args.strict:
            return 1
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as exc:
        print(f"BRAIN_V2_ERROR: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
