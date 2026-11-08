#!/usr/bin/env python3
"""MusicFlow modular test inventory and impact-driven selective regression plan.

No tests are executed by this tool. Every proposed test is read from a real
Kotlin @Test method in this checkout; stale references fail validation.
"""

from __future__ import annotations

import argparse
from collections import defaultdict
from dataclasses import dataclass
from fnmatch import fnmatchcase
import json
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "qa" / "impact_modules.json"
CATALOG = ROOT / "docs" / "Modular_Test_Catalog.md"
TEST_METHOD = re.compile(r"@Test(?:\s*\([^)]*\))?\s+(?:public\s+)?fun\s+(\w+)\s*\(")
PACKAGE = re.compile(r"(?m)^\s*package\s+([\w.]+)")


@dataclass(frozen=True, order=True)
class Case:
    module: str
    cls: str
    method: str
    kind: str
    file: str

    @property
    def id(self) -> str:
        return f"{self.module}/{self.cls.rsplit('.', 1)[-1]}#{self.method}"

    @property
    def runner(self) -> str:
        if self.kind == "android":
            return f"{self.cls}#{self.method}"
        return f"{self.cls}.{self.method}"


def matching(path: str, patterns: list[str]) -> bool:
    return any(fnmatchcase(path, pattern) for pattern in patterns)


def normalize_path(path: str) -> str:
    value = path.replace("\\", "/").strip()
    pure = PurePosixPath(value)
    if not value or pure.is_absolute() or ".." in pure.parts or ":" in pure.parts[0]:
        raise ValueError(f"Only repository-relative paths are supported: {path!r}")
    return pure.as_posix()


def load_config(root: Path = ROOT) -> dict[str, Any]:
    return json.loads((root / "qa" / "impact_modules.json").read_text(encoding="utf-8"))


def test_files(root: Path = ROOT) -> list[Path]:
    return sorted((root / "app" / "src").rglob("*Test.kt"))


def inventory(config: dict[str, Any], root: Path = ROOT) -> tuple[dict[str, list[Case]], list[str]]:
    mapping: dict[str, list[Case]] = {}
    all_files = test_files(root)
    seen_files: set[str] = set()
    for module in config["modules"]:
        cases: list[Case] = []
        for path in all_files:
            relative = path.relative_to(root).as_posix()
            if not matching(relative, module["tests"]):
                continue
            seen_files.add(relative)
            source = path.read_text(encoding="utf-8-sig")
            package = PACKAGE.search(source)
            if not package:
                raise ValueError(f"Missing Kotlin package: {relative}")
            kind = "android" if "/androidTest/" in relative else "jvm"
            for method in TEST_METHOD.findall(source):
                cases.append(Case(module["id"], f"{package.group(1)}.{path.stem}",
                                  method, kind, relative))
        if not cases:
            raise ValueError(f"Module has no mapped tests: {module['id']}")
        mapping[module["id"]] = sorted(set(cases))
    unmapped = sorted(p.relative_to(root).as_posix() for p in all_files
                      if p.relative_to(root).as_posix() not in seen_files)
    return mapping, unmapped


def validate(config: dict[str, Any], cases: dict[str, list[Case]],
             unmapped: list[str]) -> dict[str, int]:
    modules = config["modules"]
    ids = [module["id"] for module in modules]
    if len(set(ids)) != len(ids):
        raise ValueError("Duplicate module ID")
    if unmapped:
        raise ValueError("Unmapped test classes (fail closed): " + ", ".join(unmapped))
    for module in modules:
        module_id = module["id"]
        if module_id in module["requires"]:
            raise ValueError(f"Self dependency {module_id}")
        if set(module["requires"]) - set(ids):
            raise ValueError(f"Unknown dependency in {module_id}")
        if not module.get("smoke"):
            raise ValueError(f"No downstream smoke cases for {module_id}")
        valid = {f"{case.cls.rsplit('.', 1)[-1]}#{case.method}" for case in cases[module_id]}
        for smoke in module["smoke"]:
            if smoke not in valid:
                raise ValueError(f"Missing/stale smoke test: {module_id}/{smoke}")
    edges = {module["id"]: module["requires"] for module in modules}

    def visit(node: str, ancestors: set[str]) -> None:
        if node in ancestors:
            raise ValueError(f"Circular module dependency at {node}")
        for dep in edges[node]:
            visit(dep, ancestors | {node})

    for item in ids:
        visit(item, set())
    return {
        "modules": len(ids),
        "unique_test_classes": len({case.file for group in cases.values() for case in group}),
        "unique_test_methods": len({(case.file, case.method) for group in cases.values()
                                     for case in group}),
        "module_case_links": sum(len(group) for group in cases.values())
    }


def downstream(module_ids: set[str], config: dict[str, Any]) -> set[str]:
    impacted: set[str] = set()
    pending = set(module_ids)
    modules = config["modules"]
    while pending:
        node = pending.pop()
        for module in modules:
            if node in module["requires"] and module["id"] not in module_ids | impacted:
                impacted.add(module["id"])
                pending.add(module["id"])
    return impacted


def changed_files(root: Path = ROOT) -> list[str]:
    def run(args: list[str]) -> list[str]:
        p = subprocess.run(["git", "-C", str(root), *args], capture_output=True,
                           text=True, encoding="utf-8", errors="replace", check=True)
        return [line.strip() for line in p.stdout.splitlines() if line.strip()]
    paths = run(["diff", "--name-only", "HEAD"])
    paths += run(["ls-files", "--others", "--exclude-standard"])
    return sorted(set(paths))


def select(config: dict[str, Any], cases: dict[str, list[Case]], *,
           files: list[str] | None = None, modules: list[str] | None = None,
           full: bool = False) -> dict[str, Any]:
    paths = sorted(set(normalize_path(p) for p in (files or [])))
    registry = {module["id"]: module for module in config["modules"]}
    direct = set(modules or [])
    unknown_modules = direct - set(registry)
    if unknown_modules:
        raise ValueError("Unknown module IDs: " + ", ".join(sorted(unknown_modules)))

    unclassified: list[str] = []
    non_runtime: list[str] = []
    risk_reasons: list[str] = []
    for path in paths:
        hits = set()
        for module in config["modules"]:
            if matching(path, module["source"]) or (
                "/src/test/" in path or "/src/androidTest/" in path
            ) and matching(path, module["tests"]):
                hits.add(module["id"])
        direct.update(hits)
        if matching(path, config["global_full_patterns"]):
            risk_reasons.append(f"Shared build/manifest/navigation/theme: {path}")
        if matching(path, config["high_risk_patterns"]):
            risk_reasons.append(f"High-risk security/data/upgrade path: {path}")
        if not hits:
            if matching(path, config["non_runtime_patterns"]):
                non_runtime.append(path)
            elif not matching(path, config["global_full_patterns"]):
                unclassified.append(path)
    if unclassified:
        risk_reasons.append("Unclassified change needs impact review: " +
                            ", ".join(unclassified))
    if len(direct) >= 4:
        risk_reasons.append("Four or more directly changed modules")
    must_full = bool(full or risk_reasons)
    if full:
        risk_reasons.append("Explicit full regression requested")
    if must_full:
        # Unknown/shared changes cannot be considered covered by a small test subset.
        direct_selection = set(registry)
        impacted = set()
    else:
        direct_selection = direct
        impacted = downstream(direct, config)

    selected: list[dict[str, str]] = []
    for module_id in sorted(direct_selection):
        for case in cases[module_id]:
            selected.append({"id": case.id, "class": case.cls, "method": case.method,
                             "kind": case.kind, "module": module_id,
                             "reason": "direct/full"})
    for module_id in sorted(impacted):
        smoke_set = set(registry[module_id]["smoke"])
        for case in cases[module_id]:
            if f"{case.cls.rsplit('.', 1)[-1]}#{case.method}" in smoke_set:
                selected.append({"id": case.id, "class": case.cls,
                                 "method": case.method, "kind": case.kind,
                                 "module": module_id, "reason": "dependent-contract"})

    # A test belonging to several modules should only run once.
    unique: dict[tuple[str, str, str], dict[str, str]] = {}
    for item in selected:
        unique.setdefault((item["class"], item["method"], item["kind"]), item)
    selected = sorted(unique.values(), key=lambda x: (x["kind"], x["class"], x["method"]))
    extras = sorted({cmd for module_id in direct_selection | impacted
                     for cmd in registry[module_id].get("extra", [])})
    return {
        "changed_paths": paths,
        "direct_modules": sorted(direct),
        "affected_modules": sorted(impacted),
        "mode": ("full_required" if must_full else
                 "selective" if direct else "qa_docs_only" if non_runtime else "no_changes"),
        "risk_reasons": risk_reasons,
        "unclassified": unclassified,
        "test_count": len(selected),
        "testcases": selected,
        "extra_commands": extras,
        "non_runtime": non_runtime,
        "explanation": (
            "Full suite required: security/data/schema/shared/unknown changes or explicit request."
            if must_full else
            "Direct modules: all mapped tests. Transitive consumers: smoke/contract tests. "
            "Unrelated modules are not in the dependency closure; do not run them unless "
            "failures, new evidence, release gates, or uncovered integrations demand it."
            if direct else
            "Documentation/QA-only: validate the QA tool and Brain; no application runtime "
            "regression is claimed or required by source impact."
        ),
    }


def commands(plan: dict[str, Any]) -> list[str]:
    android: dict[str, list[str]] = defaultdict(list)
    jvm: dict[str, list[str]] = defaultdict(list)
    for item in plan["testcases"]:
        key = item["class"]
        (android if item["kind"] == "android" else jvm)[key].append(item["method"])
    lines: list[str] = []
    for cls, methods in sorted(jvm.items()):
        # JUnit runner selection by method is supported by Gradle --tests.
        patterns = " ".join(f'--tests "{cls}.{method}"' for method in methods)
        lines.append(f".\\gradlew.bat :app:testDebugUnitTest {patterns}")
    for cls, methods in sorted(android.items()):
        if cls.endswith(".SignedApkUpgradeV6Test"):
            # Both steps require an isolated AVD, APK signatures, seed before
            # verify, and install -r between them. Running either alone is wrong.
            lines.append(
                ".\\scripts\\run-signed-upgrade-v8-qa.ps1 -Serial emulator-5580"
                "  # Dedicated fresh QA AVD; run full two-stage upgrade"
            )
            continue
        for method in methods:
            lines.append(
                "adb -s emulator-5580 shell am instrument -w -r -e class "
                f"{cls}#{method} "
                "com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner"
            )
    lines.extend(plan["extra_commands"])
    if plan["mode"] == "qa_docs_only":
        lines += [
            "py -3 -m unittest discover -s scripts -p test_impact_regression.py",
            "py -3 scripts/project_brain_v2.py check --strict",
        ]
    return lines


def catalog(config: dict[str, Any], cases: dict[str, list[Case]]) -> str:
    lines = [
        "# MusicFlow — Modular Test Catalog & Impact Map",
        "",
        "> Generated from actual `@Test` methods by `py -3 scripts/impact_regression.py catalog`. ",
        "> Canonical QA map: `qa/impact_modules.json`. The cross-session brain points here; this is not another brain.",
        "",
        "**Selection rule:** direct module = all cases; transitive dependent module = smoke/contract cases; ",
        "shared build/core contract/unknown/high-risk = full review; mapped UI component changes select downstream consumers. Tests require their appropriate runner; this catalog ",
        "does not attest that they passed. IDs are `MODULE/Class#method` and do not renumber.",
        "",
        "## Module coverage summary",
        "",
        "| Module | Area | Cases linked | Depends on |",
        "|---|---|---:|---|"
    ]
    for module in config["modules"]:
        key = module["id"]
        lines.append(f"| `{key}` | {module['name']} | {len(cases[key])} | " +
                     ", ".join(module["requires"]) + " |")
    lines += ["", "## Test cases by module", ""]
    for module in config["modules"]:
        key = module["id"]
        lines += [
            f"### {key} — {module['name']}",
            "",
            "**Source paths:** " + ", ".join(f"`{x}`" for x in module["source"]),
            "",
            "**Downstream contract/smoke:** " + ", ".join(
                f"`{key}/{x}`" for x in module["smoke"]) + ".",
            "",
        ]
        files: dict[str, list[Case]] = defaultdict(list)
        for case in cases[key]:
            files[case.file].append(case)
        for path, grouped in sorted(files.items()):
            lines += [f"**{grouped[0].cls.rsplit('.', 1)[-1]}** (`{path}`; {grouped[0].kind})", ""]
            for case in grouped:
                lines.append(f"- `{case.id}`")
            lines.append("")
        lines.append("")
    lines += [
        "## Policy and usage",
        "",
        "`py -3 scripts/impact_regression.py validate` — fail on test class not mapped, stale smoke, or dependency cycle.",
        "",
        "`py -3 scripts/impact_regression.py plan --files app/src/main/java/com/example/musicflow/feature/search/SearchViewModel.kt`",
        "",
        "`py -3 scripts/impact_regression.py plan --git-diff --json` — analyze current changes, including untracked files.",
        "",
        "`py -3 scripts/impact_regression.py plan --module SEARCH` — explicitly supplied direct module.",
        "",
        "Before code edits record direct modules, dependents, selected case IDs and their reasons. ",
        "After running report PASS/FAIL/SKIP with command/SDK/log evidence. Failure or uncovered runtime changes ",
        "expand the selection; never call a planned test PASS. Full regression is mandatory for releases, ",
        "security/data/SAF/schema/shared changes, unknown sources, or unresolvable side effects. Android ",
        "instrumentation requires an isolated QA AVD; physical-device TalkBack/keyboard is manual acceptance. ",
        "SignedApkUpgradeV6Test is a paired seed/verify test: always use the signed upgrade script.",
        "",
    ]
    # Never emit trailing whitespace: git diff --check and CI compare the
    # generated artifact byte-for-byte on Windows and Linux.
    return "\n".join(line.rstrip() for line in lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subs = parser.add_subparsers(dest="command", required=True)
    subs.add_parser("validate", help="Check module mapping, test coverage and smoke cases")
    subs.add_parser("catalog", help="Regenerate docs/Modular_Test_Catalog.md")
    plan_parser = subs.add_parser("plan", help="Impact plan only (NEVER executes tests)")
    plan_parser.add_argument("--files", nargs="+", default=[], help="Changed repository paths")
    plan_parser.add_argument("--module", action="append", default=[], help="Direct module ID")
    plan_parser.add_argument("--git-diff", action="store_true", help="Include changed/untracked Git paths")
    plan_parser.add_argument("--full", action="store_true", help="Force full suite")
    plan_parser.add_argument("--json", action="store_true", help="Emit machine-readable plan")
    args = parser.parse_args()
    try:
        conf = load_config()
        items, unmapped = inventory(conf)
        stats = validate(conf, items, unmapped)
        if args.command == "validate":
            print("MODULAR_QA_MAP_VALID=PASS " + json.dumps(stats))
        elif args.command == "catalog":
            CATALOG.write_text(catalog(conf, items), encoding="utf-8", newline="\n")
            print("MODULAR_QA_CATALOG_UPDATED=" + str(CATALOG.relative_to(ROOT)))
            print(json.dumps(stats))
        else:
            files = list(args.files)
            if args.git_diff:
                files += changed_files()
            result = select(conf, items, files=files, modules=args.module, full=args.full)
            if args.json:
                result["commands"] = commands(result)
                print(json.dumps(result, ensure_ascii=False, indent=2))
            else:
                print("MODE=" + result["mode"])
                print("DIRECT=" + ", ".join(result["direct_modules"]))
                print("DEPENDENTS=" + ", ".join(result["affected_modules"]))
                print("WHY=" + result["explanation"])
                for risk in result["risk_reasons"]:
                    print("FULL_REASON=" + risk)
                print("CASE_COUNT=" + str(result["test_count"]))
                for case in result["testcases"]:
                    print("CASE=" + case["id"] + " (" + case["reason"] + ")")
                print("COMMANDS (not executed):")
                for command in commands(result):
                    print("  " + command)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print("MODULAR_QA_ERROR=" + str(error), file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
