# SDD ledger — plan: docs/superpowers/plans/2026-09-27-language-core.md

Plan commit: f34c5a8. Spec: docs/superpowers/specs/2026-09-27-language-core-design.md
Execution: workflow, units [0+1] [2] [3+4] [5] [6] [7] [8+9] [10+11] [12]. Pre-flight scan runs in parallel with unit 0+1; its rulings go to rulings.md, which every later dispatch reads.
Ruling: tasks batched into 9 units at the user's request ("batch work efficiently") — fewer dispatches and reviews — if wrong, a unit's review surface is larger and a fix round covers two tasks.
Workflow run: wf_0523d7e5-41f (script under ~/.claude/projects/F--InteliJ-Programs-CoolStuffLib/.../workflows/scripts/language-core-sdd-wf_0523d7e5-41f.js). Models: implementers sonnet (haiku for units 3 and 7), reviewers sonnet, rulings/escalation/final review opus.
Ruling: pre-flight scan runs alongside unit 0+1 instead of before it — Task 0 only records current output and Task 1 only adds new files — if wrong, a ruling on Task 1 is applied as a fix during unit 2.

Unit 1: implemented (f34c5a8..d2a5122) — Task 0: 2/2 (record + replay) PASS; Task 1: 20/20 PASS after fix; full suite: 469/469 passing, output pristine
Pre-flight: preflight-codebase.md clean; preflight-crosstask.md found one defect (Task 2 section comment test) and one design tension (Task 4 copy).
Ruling: a folded section's comment (Items.Heal for items/Heal.yml) is the file's header block (comments, then an empty line), written only when the file is created and read back by SplitYamlBackend and ClasspathLanguageSource — keeps the brief's round-trip test and the spec's "sections keep their comments" — if wrong, owners see one header comment per section file, which is harmless to remove.
Ruling: LanguageMigration.copy keeps aborting on any source read problem — a partial copy followed by the backup-and-remove of the old file would lose the broken file's keys — if wrong, an owner with one broken legacy file keeps the old layout until they fix that file.

Unit 1: parked W1 — Ruling: parked — no current AdminPanel language key is literally 'y', 'n', 'Y', or 'N' (checked both golden fixtures src/test/resources/golden/adminpanel/en.yml and de.yml with `grep '^\s*[yYnN]:'`, zero matches in either file), so the RESERVED set's exclusion of bare y/n (needed to pass missingSectionsAreCreatedWithTheirComments) is not exercised by any real key today, and the Bukkit-YamlConfigur
Unit 1: parked W2 — Ruling: parked — verified byte-identical by direct comparison rather than left as unverifiable. en.yml: read both C:\Users\quiri\IdeaProjects\AdminPanel\src\main\resources\languages\en.yml and F:\InteliJ Programs\CoolStuffLib\src\test\resources\golden\adminpanel\en.yml in full (1835 lines each) — every line matches. de.yml: line counts match exactly (1720 each in both C:\Users\quiri\IdeaProjects\A
Unit 1 (Task 0+1): complete (commits f34c5a8..d2a5122, review clean)

Unit 2: implemented (d2a5122..31cc658) — 480/480 passing (full suite), output pristine; focused SplitRuleTest/SplitYamlBackendTest/YamlDocumentTest 31/31 passing
Unit 2: minor (deferred): SplitYamlBackend.java:289-297 — when write() creates a brand-new section file with a header it writes the header directly via Files.writeString (no temp-file/atomic-move, no SnakeYAML validation), then calls YamlEntries.merge for the entries in a separate step. A crash between the two writes, or a later validation failure in merge, leaves a header-only file on disk (comment block + blank line, no
Unit 2: parked W1 — Ruling: parked — YamlDocument's own default newline for a brand-new file matches the "\n" SplitYamlBackend hardcodes for the section header, verified directly against YamlDocument.java (Task 1, already landed at HEAD 31cc658, so this is checkable now despite being outside the Unit 2 diff). YamlDocument.newline() (YamlDocument.java:202-210) counts existing \r\n vs \n endings and returns "\n" on a 0
Unit 2: Ruling on W2: fix — SplitRule.sectionOf (src/main/java/de/happybavarian07/coolstufflib/languagemanager/storage/SplitRule.java:54) is declared public, but should be package-private. Task 3's brief (task-3-brief.md) creates LegacyYamlBackend.java and ClasspathLanguageSource.java in `storage/`, the same directory convention Task 2 used, which resolved to package de.happybavarian07.coolstufflib.languagemanager.storage — 

Unit 2: minor (deferred, out of scope): Task 3's ClasspathLanguageSource.read still needs to add SplitRule.sectionOf/YamlEntries.readHeader-based header reading in its split-file branch, per the ruling appended for Task 3 in rulings.md — this is future Task 3 implementation work, not part of this fix diff.
Unit 2: fix round 1/5 (1 addressed, 0 open; commits 31cc658..fb202b7)
Unit 2 (Task 2): complete (commits d2a5122..fb202b7, review clean)

Unit 3: implemented (fb202b7..e4046d3) — 485/485 passing, output pristine

Unit 3: minor (deferred): ClasspathLanguageSource.java:58-66 — each split resource's text is fetched twice (once in readInto, once for the header check); redundant read, not a correctness issue.
Unit 3 (Task 3+4): complete (commits fb202b7..e4046d3, review clean)
Unit 4: implementer and its opus escalation both died on the account session limit, no work left behind (tree clean at e4046d3). Run wf_0523d7e5-41f halted; restarted as a new run with startUnit 4, base e4046d3.
Run wf_9db0a114-29a started at unit 4 (same script file).
Unit 4: implemented (e4046d3..0c92a77) — 490/490 passing, output pristine

Unit 4: parked W1 — Ruling: parked — Tasks 1-4 (LanguageBackend, ReadResult, MigrationReport, LanguageMigration, ClasspathLanguageSource, SplitYamlBackend, LegacyYamlBackend) were already reviewed and approved in Units 1-3 (see rulings.md entries for Unit 1 C1/C2/W1/W2, Unit 2 W1/W2, and the Task 3/Task 4 rulings); this unit's diff (task-5-brief.md, LanguageStorage.java) only calls their public API (LanguageBackend.r
Unit 4: parked W2 — Ruling: parked — the controller's instructions for this review explicitly say not to re-run the reported full suite absent a specific code doubt; unit-4-report.md documents RED (compilation failure before Step 3) then three GREEN runs (LanguageStorageTest 5/5, the four integration tests 16/16, full suite 490/490) with plausible, non-suspicious output (only expected INFO/WARNING migration log lines
Unit 4 (Task 5): complete (commits e4046d3..0c92a77, review clean)

Unit 5: implemented (0c92a77..55abc7e) — 494/494 passing (490 prior + 4 new LanguageFallbackTest), output pristine
Unit 5: parked W1 — Ruling: Task 7 does not need any variant handling beyond what Task 6's textOf already covers — Task 7's own brief (task-7-brief.md, Step 5 `send`) resolves `actionbar`/`title`/`subtitle`/`sound` by reading the rich ConfigurationSection directly (`rich.isString("actionbar")`, `rich.getString("actionbar")`, etc.), never through `textOf`; `renderLines` reads the raw List value directly too. `MessageB
Unit 5 (Task 6): complete (commits 0c92a77..55abc7e, review clean)

Unit 6: implemented (55abc7e..aa366dc) — 497/497 passing (494 prior + 3 new MessageBuilderTest), output pristine
Unit 6 (Task 7): complete (commits 55abc7e..aa366dc, review clean)

Unit 7: implemented (aa366dc..d8d0648) — 501/501 passing, output pristine
Unit 7: Ruling on F1: fix — src/main/java/de/happybavarian07/coolstufflib/languagemanager/PerPlayerLanguageHandler.java:51-54: getPlayerLanguage returns null for a player with no stored language. The old code (diff line 342) fell back to lgm.getCurrentLangName(). This breaks the compatibility contract (spec:28,40). It also changes LanguageManager.getLangOrPlayerLang (line 880) today, so the reviewer's 'no regression' claim is wrong: with currentLang=false and a player with no stored language, the old code returned the current language and the new code returns null. Replace the body with `return lgm.getLang(getPlayerLanguageName(uuid), false);`. This restores the fallback for an unset language and still returns null, without throwing, when the stored language is no longer registered. Update the javadoc to match. Recorded in rulings.md (affects Task 10+ render paths and the golden test).
Unit 7: Ruling on F2: fix — src/main/java/de/happybavarian07/coolstufflib/languagemanager/LanguageManager.java:258-271 (createMigratorForLanguage): the writer builds `new LanguageEntry(key, value, null, null)`, so a MISSING_IN_USER key is inserted without the jar's comment. That violates spec:58 ('carried along by migration'). Smallest fix, which leaves the migrator API unchanged: in the writer lambda, use `LanguageEntry jar = loaded.defaults().get(e.getKey());` and pass `jar == null ? null : jar.comment()` as the comment. YamlDocument.set (YamlDocument.java:97-124) only uses the comment on insertion, so DIFFERENT_VALUE and edited keys are unaffected. This matches LanguageConfig.saveConfig:54, which already carries known.comment(). Keep LanguageFileMigrator's Map<String, Object> API as the brief says. Optionally extend LanguageFileMigratorTest or add a LanguageManager-level test showing that a migrated missing key gets the jar comment. Recorded in rulings.md.
Unit 7: Ruling on F3: fix — LanguageFileMigrator.java:212-213: keep the implementer's change, where MigrationEntry pre-selects only MISSING_IN_USER. The brief's own Step 1 test cannot pass without it. Pre-selecting DIFFERENT_VALUE would also make migrateSelected rewrite, through config.write, owner value lines that did not change, which requotes them. The one fix is disclosure: unit-7-report.md 'Deviations from Brief: None' must list this change and why it was made. No code change. Recorded in rulings.md as superseding task-9-brief.md's 'keep MigrationEntry unchanged'.
Unit 7: parked W1 — Ruling: parked because the question is moot. — The F1 fix restores the old fall-back-to-current-language result for every caller, internal or external, so no consumer can depend on the null behaviour. A grep of AdminPanel/src found no direct getPlayerLanguage callers, and the only in-repo caller is LanguageManager.java:880. — If wrong: none, since the F1 fix brings back the pre-overhaul result.
Unit 7: parked W2 — Ruling: not a gap. — LanguageConfig.saveConfig (LanguageConfig.java:44-58) already carries known.comment() from base.merged() for keys changed through getConfig(). The migrator never goes through saveConfig: createMigratorForLanguage calls config.write directly (LanguageConfig.java:71). So saveConfig neither hides F2 nor has its own comment gap, and F2 is fixed on the migrator path. — If wrong: a test showing F2 is not fixed would fail.
Unit 7: parked W3 — Ruling: accept the reported test run. — My role is read-only with no builds. The report is consistent with the diff, and the F1/F2 fixes need a fresh run of `mvn -B test -Dgpg.skip -Dtest=PlayerLanguageStoreTest+LanguageFileMigratorTest+GoldenLanguageOutputTest+LanguageManagerTest` plus the full suite anyway, which will re-verify it. — If wrong: a failing test shows up in that required re-run, before completion.

Unit 7: minor (deferred): unit-7-report.md:5 still reads 'No deviations from briefs' in the top Summary, contradicting the '## Deviations from Brief' section (lines 106-118) that the fix round added to disclose the MigrationEntry pre-selection change (F3). The Summary line was not updated, leaving the report internally inconsistent about whether a deviation exists.

Unit 7: fix round 1/5 (3 addressed, 1 open — R1N1; commits d8d0648..b172c64)
Unit 7: fix round 2/5 (1 addressed, 0 open — R1N1; test coverage gap closed)
Unit 7: fix round 3/5 (0 addressed, 1 open — R1N1; commits 7519947..HEAD)
Unit 7: fix round 3/5 (0 addressed, 1 open — R1N1; commits 7519947..68b56f2)
