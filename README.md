# CoolStuffLib

A comprehensive Java library for plugin and application developers, providing advanced command management, menu systems, multi-language support, configuration utilities, and a custom JPA-inspired persistence system. Designed for rapid integration and extensibility.

---

## 🚀 Features

- **Command Manager**: Subcommands (also nested), typed arguments, generated usage and tab completion, help pages, cooldowns, confirmations, async commands and builder-made commands.
- **Command Aliases**: Command shortcuts, also created at runtime and stored in `data.yml` or a database table.
- **Menu System**: Buttons with chainable click actions (permission, confirmation, cooldown, sound, commands, chat prompts), list menus, pagination zones and in-place refresh.
- **Language Manager**: Effortless multi-language support for your projects.
- **Config Manager**: Streamlined configuration file handling.
- **Expression Engine**: Dynamic expression evaluation (e.g., math operations).
- **Utils Class**: Handy utility methods for common tasks.
- **Plugin File Logger**: Dedicated logging for plugin-specific events.
- **Player Menu Utility**: Context-aware menu operations for players.
- **PlaceholderAPI Support**: Automatic integration if PlaceholderAPI is present.
- **Startup Hooks**: Inject custom logic during core system initialization.
- **Persistent Data File Handling**: Centralized storage for plugin data.
- **Custom JPA System**: Project-specific persistence layer inspired by JPA, providing entity management, repository patterns, and data operations tailored for plugins and modular applications.

---

## 🏗️ Core Architecture

- **Command System**: Modular command registration, parsing, and execution.
- **Menu System**: Dynamic menu creation and player interaction utilities.
- **Language System**: Multi-language support with runtime switching and translation utilities.
- **Config System**: Hierarchical configuration management and migration utilities.
- **Expression Engine**: Runtime evaluation of mathematical and logical expressions.
- **Persistence System**: Custom JPA-like framework for entity and repository management (see `src/main/java/de/happybavarian07/coolstufflib/jpa`).
- **Utility Modules**: Logging, file handling, and plugin lifecycle hooks.

---

## 📈 Planned Updates

Full list: [docs/ROADMAP.md](docs/ROADMAP.md).

- **Language Translator**: Translate language files using custom APIs or Google Translate.
- **File Manager**: Advanced file and directory management.
- **Internal Event Bus & Dependency Injection**: Decouple subsystems and simplify testing.
- **Menu Templates & Animations**: Reusable menu presets and transitions.

> Already shipped (previously listed as planned): MySQL/MariaDB access via the custom JPA
> persistence layer, a multi-format advanced configuration framework
> (`configstuff/advanced`, incl. validation, migration, versioning, history, encryption),
> and data backup/restore via `backupmanager`.

---

## 🛠️ Quick Start

1. **Add CoolStuffLib to your project**
   - See [Maven Central](https://central.sonatype.com/artifact/io.github.happybavarian07/CoolStuffLib) for the latest version.
2. **Integrate features**
   - Refer to the [Wiki Documentation](https://happybavarian07.gitbook.io/wiki/coolstufflib/coolstufflib) for setup and usage examples.

---

## 📚 Documentation & Examples

- [Wiki](https://happybavarian07.gitbook.io/wiki/coolstufflib/coolstufflib)
- [Tutorials](docs/tutorials/cool-stuff-lib-tutorial.md)

---

## 🤝 Contributing

Contributions are welcome! Open issues, submit pull requests, or join the Discord (link available on Spigot plugins and the Admin-Panel repo).

---

## 📄 License

MIT License. See [LICENSE](LICENSE) for details.

---

**Need help or have questions?**
- Open an issue on GitHub
- Join the Discord community

Happy coding!
