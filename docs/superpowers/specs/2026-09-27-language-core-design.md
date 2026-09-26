# Language Core — Design

Date: 2026-09-27. Release: CoolStuffLib 3.0.0 (part of the overhaul, before the release).
Status: approved in chat (storage, retrieval; owner tools as outlined).

## Why

The language manager reads its YAML on every call (two config lookups, a regex over the text, parsing embedded
expressions again every time), keeps placeholders in one global map shared by all threads, and rewrites the server
owner's file on every start through `ConfigUpdater`. That rewrite drops comments and, as found on 2026-09-26, can make
the whole file unreadable; one broken line then turns every message into `null path: ...`. The file itself is one
1700-line YAML file per language that owners have to search by hand.

## Scope

This is sub-project 1 of 5:

1. **Language core (this spec):** backends and migration, compiled snapshots, per-call rendering, owner tools.
2. Network store and live editing: SQL backend, in-game editor, cross-server invalidation, edit history.
3. Translation lifecycle: stale detection by source hash, coverage report, machine fill with review queue.
4. Rich messages beyond this spec: hover/click, ICU plural/select, locale-aware number and date formatting.
5. Typed keys: build-time generated message API (optional).

Not in this spec: anything of 2–5, except that the entry format and the backend interface must not block them.

## Compatibility contract

Everything below keeps compiling and returns the same results for the same language file content:

- `LanguageManager`: `getMessage`, `getMessageOrDefault`, `getItem` (all overloads, `MaterialCondition`),
  `getMenuTitle`, `getCustomObject`, `addPlaceholder`/`addPlaceholders`/`resetPlaceholders`/`replacePlaceholders`,
  `reloadLanguages`, `updateLangFiles`, `addLang`/`getLang`/`getRegisteredLanguages`/`setCurrentLang`,
  `getPLHandler`/`setPLHandler`, `createMigratorForLanguage`, expression variables and `parseEmbeddedExpressions`.
- `LanguageFile(File folder, String resourceDirectory, String name)`, `getLangName()`, `getFullName()`,
  `getFileVersion()`. `getLangFile()` returns the language's folder once the split layout is used.
- `LanguageConfig#getConfig()` returns a read-only `YamlConfiguration` view built from the snapshot, so code that reads
  it keeps working. `saveConfig()` writes the changed keys through the backend.
- `LanguageFileMigrator` (used by AdminPanel's language migration menus) compares the owner's entries with the jar's
  through the backends instead of two files.
- `PerPlayerLanguageHandler` keeps its constructor and methods and the `playerdata.<uuid>.language` layout in `data.yml`.

Two intended behaviour changes, both bug fixes:

- `addPlaceholder` overwrites an existing value for the same key (L3).
- Every lookup accepts the key with or without the `Messages.` root, for every kind of value (L4).

## 1. Storage

### LanguageEntry

`record LanguageEntry(String key, Object value, String comment, Location origin)`

- `key`: full dotted path from the file root, e.g. `Messages.Player.General.NoPermissions`, `Items.StartMenu.HintItem`,
  `MenuTitles.StartMenu`, plus the header keys (`LanguageFullName`, ...).
- `value`: `String`, `List<String>`, or `Map<String, Object>` (items, rich entries, plural variants, custom objects).
  An entry is a leaf of the YAML tree, except that item sections, rich entries and plural maps are one entry each (see
  "Entry kinds").
- `comment`: the comment lines directly above the key, without `#`; carried along by migration and merge.
- `origin`: backend id, file and line, used in error messages and in `/lang debug`.

### LanguageBackend

```java
public interface LanguageBackend {
    String id();                                        // "yaml-split", "yaml-legacy", later "sql", ...
    Set<String> languages();
    ReadResult read(String language);                   // entries + errors, never throws for bad content
    void write(String language, Collection<LanguageEntry> entries);   // add or update, never deletes
    boolean isWritable();
}
record ReadResult(Map<String, LanguageEntry> entries, List<LanguageProblem> problems) {}
```

`read` is called off the main thread. A backend reads everything it can; a broken file yields a `LanguageProblem`
(file, line, message) and no entries from that file, while the other files load normally.

### SplitYamlBackend (default on disk)

Layout:

```
languages/
  en/
    language.yml                 # header: LanguageFullName, LanguageUUID, LanguageVersion, LanguageParent, LanguageLocales
    messages/<Section>.yml       # every key under Messages.<Section>
    items/<Section>.yml          # every key under Items.<Section>
    titles.yml                   # MenuTitles
    other.yml                    # any other root key
  de/ ...
  _backup/<yyyy-MM-dd_HH-mm>/    # legacy files after migration
```

Split rule: the root (`Messages`, `Items`, `MenuTitles`) picks the folder, the second path part picks the file. Keys in
a file are written relative to their section, so `messages/Player.yml` starts with `General:` and not with
`Messages: Player:`. A root with no second level goes into `other.yml`. The rule depends only on the key, so it works
for every plugin without configuration.

`write` uses a text merge, never load-and-save:

1. Parse the target file into a line-based key index (key → line range, indentation).
2. For every entry whose key exists: replace only the value lines of that key; keep indentation, comments and the
   quoting style of the key line.
3. For every missing key: find the nearest existing parent section and insert the entry after its last child, with the
   entry's comment and the parent's indentation + 2. Missing parents are inserted the same way.
4. Values are written as single-quoted YAML (`'` doubled); lists as `- '...'`; maps as nested blocks. Every string that
   is written is escaped by one function with tests (the `ConfigUpdater` bug came from ad-hoc quoting).
5. Parse the resulting text with SnakeYAML before writing. If it fails, write nothing and report a problem.
6. Write to `<file>.tmp` and move it over the file (atomic where the file system allows it).

### LegacyYamlBackend

Reads and writes today's single `languages/<lang>.yml` with the same text merge. It exists to read old files for the
migration and to read jar defaults of plugins that keep shipping single files.

### Jar defaults

A `JarSource` reads the plugin's `resourceDirectory` from the jar through `SplitYamlBackend` or `LegacyYamlBackend`
(detected by whether `<lang>/language.yml` or `<lang>.yml` exists). It is read-only and is the lowest layer. Plugins do
not have to change their resources to upgrade.

### Migration

- `LanguageMigration.copy(LanguageBackend from, LanguageBackend to, String language)` reads all entries from `from`
  and writes them to `to`, then reads `to` back and compares every key's value. It returns a `MigrationReport`
  (copied, skipped, differences).
- Automatic on start: if `languages/<lang>.yml` exists and `languages/<lang>/` does not, the legacy file is migrated to
  the split layout, moved to `languages/_backup/<date>/` and the result logged. Owner values are kept; comments come
  from the owner's file where the key existed, otherwise from the jar.
- `CoolStuffLibBuilder.withLanguageManager().setBackend(LanguageBackend)` selects another backend; `/lang migrate
  <backend-id>` copies all languages to it (sub-project 2 adds the SQL backend).

### Updates

After a reload, keys that exist in the jar layer but not in the writable backend are passed to `backend.write` with
their jar comment. This replaces `LanguageFileUpdater`/`ConfigUpdater` for language files. `updateLangFiles()` runs
this step on demand. The owner's existing lines are never changed by it.

## 2. Retrieval

### Layers and snapshot

Per language the layers are, highest first: writable backend (owner files) → jar. They are merged key by key into an
immutable `LanguageSnapshot` (`Map<String, CompiledEntry>`), built off the main thread.

### Entry kinds

| Kind | Source value | Compiled into |
|---|---|---|
| Text | string | `Template`: literal pieces with `&` colors translated, placeholder slots `%name%`, pre-parsed expression trees (`Parser.parse()` from the existing expression engine), `%prefix%` resolved at render |
| TextList | list of strings | list of `Template` |
| Item | map with `material`/`displayName`/`lore`/... (today's item format) | `ItemTemplate`: templates for name and lore, other fields kept; `MaterialCondition` evaluated at render |
| Rich | map with any of `text`, `actionbar`, `title`, `subtitle`, `sound` | `RichTemplate`; `text` is what the old API returns |
| Plural | map with `one` and `other` (optionally `zero`) | `PluralTemplate`; variant picked by `%count%` |
| Raw | anything else | kept as is, for `getCustomObject` |

A value that cannot be compiled (bad expression, unknown material) produces a `LanguageProblem`; the key then falls back
to the next layer's value if it compiles, otherwise it counts as missing.

### Rendering

`RenderContext` = viewer (`Player` or `CommandSender`), language, call placeholders (`Map<String, String>`), count.

1. Join the template pieces. A placeholder slot is resolved from the call placeholders, then the legacy global map of
   the matching `PlaceholderType`, then PlaceholderAPI (only for slots still open, only when PlaceholderAPI is present).
2. Evaluate the pre-parsed expressions with the existing interpreter and the path expression variables of the viewer.
3. Rich and plural templates pick their variant first, then render as above.

### Lookup order

Language of the call (player language via the per-player store, or explicit) → its `LanguageParent` (header, optional)
→ the server language. Missing everywhere: the old API returns what it returns today (`null path: ...`, the barrier
error item, the `null path` menu title); the new API returns the key and logs the miss once per key and language.

### New API

```java
lgm.message("Player.General.NoPermissions").with("%target%", name).send(sender);  // chat + rich channels
lgm.message("Server.PlayersOnline").count(online).text(player);                    // plural variant, sets %count%
lgm.message("Help.Lines").lines(player);
lgm.message("Some.Key").lang("de").text(null);
lgm.item("StartMenu.HintItem").with("%x%", x).build(player);
lgm.title("StartMenu").text(player);
```

`MessageBuilder`, `ItemBuilder` and `TitleBuilder` are small value objects; they never touch the global placeholder map
and are safe from any thread.

### Old API

The old methods build a `RenderContext` from their arguments, use the legacy global map as the placeholder source and
keep their `resetAfter` handling on that map. The global map becomes a `ConcurrentHashMap`.

### Reload

`reloadLanguages` reads all layers and compiles off the main thread, then swaps the snapshot map through one
`AtomicReference`. A file with problems keeps its keys at their last working values; the rest updates. Problems are
logged as one report per reload (file:line, message). `LanguageCache` and its scheduled cleanup are removed (L2).

### Per-player language

`PlayerLanguageStore` interface (`get`, `set`, `remove`, `all`), default `YamlPlayerLanguageStore` on the plugin's
`data.yml` with today's layout. `PerPlayerLanguageHandler` delegates to it. A missing `playerdata` section is an empty
map (L1).

## 3. Owner tools and error handling

- **`/lang` command**, registered when the plugin enables it on the builder (`withLanguageManager().enableCommands("lang")`),
  permissions `<plugin>.lang.<sub>`:
  - `/lang reload` — reload and print the problem report.
  - `/lang debug` — toggles key display for the sender: every message sent to them through the library gets the key
    and origin (`file:line`) as hover text; the console gets a `[key]` prefix.
  - `/lang find <text>` — case-insensitive search over the rendered texts of the sender's language, prints key and
    origin, at most 10 hits.
  - `/lang missing [language]` — keys that exist in the jar but not in the owner's files, and keys the owner has that
    the jar does not know.
  - `/lang migrate <backend>` — see Migration.
- **File watcher**: with `withLanguageManager().watchFiles(true)` a `WatchService` on `languages/` triggers a reload 1 s
  after the last change.
- **Start report**: after migration and the update step, one log block lists migrated languages, added keys per file
  and problems. Nothing is printed when there is nothing to report.

## 4. Testing and rollout

- Unit tests (no server): split rule; text merge keeps every untouched byte, including comments and quoting; escaping
  round trip (`'`, `"`, `\`, `&`, unicode); broken file isolation; legacy → split migration round trip; snapshot
  compile and render; fallback order; placeholder precedence; old API vs new API equality; concurrent rendering during
  a reload.
- Golden test: for every key in AdminPanel's `en.yml` and `de.yml`, the old implementation's output (captured before
  the change) equals the new implementation's output.
- CoolStuffLibTest server suite: add checks for migration on start, `/lang debug`, `/lang find`, watcher reload, and a
  broken file that must not take the language down.
- AdminPanel is the first consumer: start on the test server with its current single files, check the migration,
  the smoke test and the in-game language switch.
- Wiki: the Languages guide and the "Migrating from 2.x" page get a section on the new layout, the builder options and
  `lgm.message(...)`.

## Risks

- `LanguageConfig#getConfig()` as a read-only view: code that writes to it and calls `saveConfig()` is supported by
  diffing the view against the snapshot; code that writes without saving loses the change on reload, as today.
- The text merge must understand the YAML subset the language files use (block mappings, block lists, single/double
  quoted and plain scalars, comments). Flow style (`[a, b]`, `{a: b}`) is read, but written values always use block
  style. Anything else it cannot place makes `write` report a problem instead of guessing.
- Pre-parsing every expression on load moves that cost to reload time; with ~2000 keys this is expected to stay far
  below a second, measured in the tests.
