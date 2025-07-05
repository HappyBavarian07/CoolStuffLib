# CoolStuffLib TODOs

## Planned Features

1. **Modular Addon System**
   - API for third-party modules/addons to register and extend core systems (commands, menus, language, etc.) at runtime, with isolation and lifecycle management.

2. **Enhanced Error Handling & Reporting**
   - Centralized error reporting with detailed logs, user-friendly messages, and optional integration with external error tracking services.

3. **Menu System Extensions**
   - Animated menus and transitions.
   - Menu templates and presets for common patterns.
   - Accessibility improvements (keyboard navigation, screen reader hints).

4. **Data Backup/Restore**
   - Scheduled automatic backups with configurable retention.
   - Simple restore commands for admins.
   - Use available backup system from Admin-Panel.

## Additional Suggestions

5. **Scripting Support** (under consideration and not really at top priority)
   - Allow plugins or users to define custom scripts (e.g., JavaScript, Groovy) for advanced behaviors and automation.

6. **Centralized Event Dispatcher**
   - Internal event bus to decouple systems and allow for flexible event-driven architecture within the library.

7. **Dependency Injection Support**
   - Provide lightweight dependency injection for easier testing and modularity.

8. **Advanced Config Validation & Management**
   - Introduce "advanced" package to "configstuff" housing the AdvancedConfigManager with sub-packages for:
     - Auto config generation.
     - Default config handling.
     - Support for multiple types of config files (not just .yml).
     - Support for config file validation.
     - Support for config file backups by integrating with the backup system.
     Maybe:
     - Support for config file history and rollback.
     - Support for config file versioning and migration.
