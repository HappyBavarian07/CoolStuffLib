# CoolStuffLib Roadmap

Last reviewed: 2026-09-23. Single source for planned work; replaces the old `TODO.md` (in the
source tree), `Menu_Item_ID_System.md` and `anticheat_menu_roadmap.md`. Verify against the code
before relying on an item's status.

## Planned

- **Modular addon system for the library** – runtime registration of third-party extensions to
  commands, menus and language handling, with isolation and lifecycle management.
- **Centralized error reporting** – structured error records and an optional external sink on top
  of `CoolStuffLib.logError`.
- **Menu templates and animations** – reusable presets (settings pages, forms), transitions and
  accessibility hints.
- **Scheduled backup/restore** – configurable retention and admin restore commands on top of
  `backupmanager`.
- **Internal event bus** – decouple subsystems through events instead of direct calls.
- **Dependency injection** – lightweight constructor injection for services and managers.
- **Config history/rollback UI** – admin-facing rollback on top of the History/Versioning modules.
- **Menu addon button persistence** – store addon-provided buttons (`AddonButtonSpec` idea:
  handler id, slot, item) in JSON so they survive menu recreation and plugin reloads.
- **Language translator** – translate language files through a pluggable translation API.
- **Scripting support** (low priority) – Groovy/JavaScript hooks for automation.

## Shipped (formerly on this list)

- Button registration API for menus (`registerButton`, `slotActions`, `forbiddenSlots`,
  `MenuListener` routing) – replaces the PDC item-id dispatch idea.
- Advanced configuration framework (`configstuff/advanced`: multiple file types, validation,
  migration, versioning, history, encryption, backups).
- MySQL/MariaDB and SQLite persistence through the JPA layer.
