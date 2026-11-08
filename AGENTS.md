# MusicFlow — AI handoff entrypoint

**Read `docs/Cross_Session_Project_Brain.json` first.** It is the SINGLE canonical memory for goals, priorities P1–P5, proven lessons, assumptions and Brain v2 evidence records. The script and docs are helpers, not another brain.

At the start of every new AI session:
1. Open that JSON file and run `python scripts/project_brain_v2.py check`. Pay special attention to `verified_repo`, `verified_local`, `unavailable`, `stale` and `contradicted`. Missing gitignored QA logs on another machine are **not** independent proof.
2. Choose the highest priority in `brain_v2.decisions` / `core_essence.priority_index`; deterministic scores use risk, user value, evidence gap, urgency and effort. P1 needs a verified blocker; no speculative P1.
3. Inspect actual Git HEAD, source and targeted QA docs as needed. Implement the feature batch, then run consolidated QA (separate Android versions and signed APK smoke). Never claim a test is PASS based only on a documentation claim.
4. At the end of EVERY development session: add evidence (Git commit/file anchors for source, QA markers for local logs), revise factors/assumptions, and record the observed lesson using `python scripts/project_brain_v2.py review --decision ID --outcome success|failure|partial --evidence ID --lesson "brief verified learning"` when proof exists. If proof is missing, leave a pending assumption instead of inventing success.
5. Run `python scripts/project_brain_v2.py sync` to recalculate P1–P5 and prune exact duplicate lessons, then `python scripts/project_brain_v2.py check` and `python -m unittest discover -s scripts -p test_project_brain_v2.py`. Increment `meta.evolution_version` after a substantive learning/update cycle and keep `brain_v2.learning_history` bounded. Do not create a second brain JSON, save secrets or discard unique lessons.
6. Commit and push code plus the SAME updated brain when authorized; verify remote sync. APKs in `dist/` and local `qa-logs/` are not in Git. Detailed usage: `docs/Project_Brain_v2.md`.

**Brain v2 is evidence-driven assisted project memory, NOT autonomous model training. Never treat unverified features, simulated billing/roles, local-only proofs or QA-signed demo APKs as production-ready.**
