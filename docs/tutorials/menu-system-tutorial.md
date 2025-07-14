# CoolStuffLib Menu System Tutorial

## Table of Contents

1. Introduction
2. Architecture Overview
3. Initialization (CoolStuffLibBuilder)
4. Core Components
5. Creating Menus
6. Menu Actions and Listeners
7. Paginated, Confirmation, Player Selector, and Casino Menus
8. Player Menu Utility
9. Menu Addons and Extensibility
10. Advanced Features
11. Example Workflow
12. Troubleshooting & Tips
13. References

---

## 1. Introduction

The CoolStuffLib Menu System provides a flexible framework for building interactive, extensible menus in Java
applications, especially for plugin-based environments. It supports custom actions, pagination, player utilities, and
addon integration.

---

## 2. Architecture Overview

- **Menu**: Base class for all menus.
- **MenuAction**: Represents actions triggered by menu interactions.
- **MenuListener**: Handles menu events.
- **PaginatedMenu**: Supports multi-page menus.
- **PlayerMenuUtility**: Manages player-specific menu state.
- **MenuAddon/Manager**: Extends menu functionality via plugins.
- **ConfirmationMenu**: Specialized menu for confirmations.

---

## 3. Initialization (CoolStuffLibBuilder)

The preferred way to initialize CoolStuffLib is via the builder pattern. This ensures all systems are properly
configured and ready for use.

```java
CoolStuffLib lib = new CoolStuffLibBuilder(plugin)
    .withMenuSystem()
    .build()
    .createCoolStuffLib();
```

You can chain other systems (language manager, command manager, etc.) as needed before calling `createCoolStuffLib()`.

---

## 4. Core Components

### Menu

Defines the structure and behavior of a menu. Extend this class to create custom menus.

```java
public class MyMenu extends Menu {
    public MyMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }
    // Implement required abstract methods
    @Override
    public String getMenuName() { return "My Custom Menu"; }
    @Override
    public String getConfigMenuAddonFeatureName() { return "MyMenuFeature"; }
    @Override
    public void handleMenu(InventoryClickEvent event) { /* ... */ }
    @Override
    public void handleCloseMenu(InventoryCloseEvent event) { /* ... */ }
}
```

### PaginatedMenu

For menus with multiple pages, extend `PaginatedMenu`:

```java
public class MyPaginatedMenu extends PaginatedMenu {
    public MyPaginatedMenu(PlayerMenuUtility util) {
        super(util);
    }
    // Implement required methods
}
```

Open a paginated menu just like a normal menu:

```java
PlayerMenuUtility util = lib.getPlayerMenuUtility(player.getUniqueId());
MyPaginatedMenu menu = new MyPaginatedMenu(util);
menu.open();
```

---

## 5. Menu Actions and Listeners

Menu actions are handled via the `handleMenu` method in your menu class. The `MenuListener` automatically routes
inventory events to your menu instance.

---

## 6. Confirmation, Paginated, Player Selector, and Casino Menus

### Confirmation Menu

Do **not** instantiate `ConfirmationMenu` directly. Use the utility method:

```java
Utils.openConfirmationMenu(
    "Are you sure?", // reason
    "MenuToOpenAfter", // menu to open after
    "menu.package", // menu package
    methodToExecuteAfter, // Method to execute
    objectToInvokeOn, // Object to invoke method on
    Arrays.asList(arg1, arg2), // Method arguments
    Arrays.asList(Exception.class), // Exceptions to catch
    player // Player
);
```

### Paginated Menus

Extend `PaginatedMenu<T>` for multi-page menus. Implement the abstract methods for paginated and custom item clicks:

```java
public class MyPaginatedMenu extends PaginatedMenu<MyType> {
    public MyPaginatedMenu(PlayerMenuUtility util) {
        super(util);
    }
    @Override
    protected void handlePageItemClick(int slot, ItemStack item, InventoryClickEvent event) {
        // Handle paginated item click
    }
    @Override
    protected void handleCustomItemClick(int slot, ItemStack item, InventoryClickEvent event) {
        // Handle custom item click
    }
}
```

You can use `setSavedMenu(Menu menu)` to set a menu to return to when closing.

### CustomPlayerSelector

`CustomPlayerSelector` is a paginated menu for selecting one or multiple players. It uses PersistentDataContainer to mark player head items for robust detection.

```java
CustomPlayerSelector<Player, ResultType> selector = new CustomPlayerSelector<>(
    action, // String describing the action
    infoItemExtraInfos, // Extra info for the info item
    playerMenuUtility, // PlayerMenuUtility instance
    functionToExecute, // Function to execute after selection
    functionForDecidingItemColor, // Function for coloring player names
    previousMenu, // Menu to return to after selection
    players, // List of Player objects
    multiSelect // true for multi-select, false for single
);
selector.open();
```

- Handles single and multi-select.
- Uses PersistentDataContainer for player head item detection.
- Returns to the previous menu or closes inventory after selection.

### CasinoMenu

`CasinoMenu` is a flexible paginated menu for casino-like item selection and animation. You can specify any item pool and handle results with custom logic, similar to CustomPlayerSelector.

```java
CasinoMenu<ItemType, ResultType> casinoMenu = new CasinoMenu<>(
    playerMenuUtility,
    itemPool, // List<ItemType> or any type you want to display/animate
    animationFunction, // Function<ItemType, ItemStack> to render each item
    resultHandler, // Function<ResultType, Void> to handle the result
    previousMenu // Menu to return to after animation or selection
);
casinoMenu.open();
```

- Specify any item pool and animation logic.
- Handle the result of the casino roll with a custom function.
- Returns to the previous menu or closes inventory after completion.

---

## 7. Player Menu Utility

`PlayerMenuUtility` is used to store and retrieve player-specific menu state. Always obtain it via:

```java
PlayerMenuUtility util = lib.getPlayerMenuUtility(player.getUniqueId());
```

You can store custom data for use across menu transitions:

```java
util.setData("key", value, true);
Object value = util.getData("key");
```

---

## 8. Menu Addons and Extensibility

Extend menu functionality by creating `MenuAddon` classes and registering them with `MenuAddonManager`.

```java
MenuAddonManager mam = lib.getMenuAddonManager();
mam.addMenuAddon(new MyMenuAddon(...));
```

Addons can hook into menu events and provide additional features.

---

## 9. Advanced Features

- Multi-menu workflows
- Dynamic menu content
- Integration with other CoolStuffLib systems

---

## 10. Example Workflow

```java
import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.CoolStuffLibBuilder;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;

// Initialization
CoolStuffLib lib = new CoolStuffLibBuilder(plugin)
        .withMenuSystem()
        .build()
        .createCoolStuffLib();

// Open a paginated menu
PlayerMenuUtility util = lib.getPlayerMenuUtility(player.getUniqueId());
MyPaginatedMenu menu = new MyPaginatedMenu(util);
menu.open();

// Open a CustomPlayerSelector
        CustomPlayerSelector<Player,ResultType>selector=new CustomPlayerSelector<>(
        "Invite Player",
        "Select a player to invite.",
        util,
        functionToExecute,
        functionForDecidingItemColor,
        previousMenu,
        Bukkit.getOnlinePlayers().stream().toList(),
        false // single select
        );
        selector.open();

// Open a CasinoMenu
CasinoMenu<ItemType, ResultType> casinoMenu = new CasinoMenu<>(
    util,
    itemPool,
    animationFunction,
    resultHandler,
    previousMenu
);
casinoMenu.open();
```

---

## 11. Troubleshooting & Tips

- Always use the builder for initialization.
- Use PlayerMenuUtility for all menu state.
- Register custom menu addons before opening menus.
- Use provided utility methods for confirmation flows.

---

## 12. References

- Source code: [CoolStuffLib GitHub](https://github.com/HappyBavarian07/CoolStuffLib)
- API docs: See project Javadocs
