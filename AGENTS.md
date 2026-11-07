# MusicFlow — AI handoff entrypoint

**Read `docs/Cross_Session_Project_Brain.json` first.** It is the single, compact source for the project's goal, priorities (P1–P5), proven lessons, unresolved assumptions and pruned noise.

At the start of every new AI session:
1. Open that JSON file. Use `core_essence.priority_index` to choose the highest-priority unfinished work that matches the user's request.
2. Cross-check claims with `git status`, the actual source, `qa-logs` and (only if needed) `docs/Ke_Hoach_Du_An.md`; the JSON is a handoff summary, not proof of test results.
3. Implement the task when asked; complete the feature batch before the comprehensive QA run.
4. **Before ending a development/update session, update the SAME JSON file in place**: recalculate P1–P5, compress confirmed successes/failures, revisit assumptions, prune stale details and increment `meta.evolution_version`. Keep the established top-level JSON schema and avoid credentials, raw logs and unnecessary history.
5. Update detailed docs/logs when applicable, but don't require the next AI to read them unless verification is necessary. Commit/push the handoff with the implementation when authorized and possible.

**Never treat unverified features, simulated billing/roles or QA-signed demo APKs as production-ready.**
