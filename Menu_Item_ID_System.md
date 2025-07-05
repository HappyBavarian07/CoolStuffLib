# Menu Item Handler System (Pseudo-Code Spec)

> ⚠️ This is **pseudo-code**. Adapt to your actual Bukkit plugin structure and naming.

## Goal

Replace monolithic `handleMenu()` logic with modular `MenuItemHandler`s that operate on items tagged by the `LanguageManager`. Use a single key in the PersistentDataContainer:

```
languagemanager:itemid -> String
```

## Overview

- The `LanguageManager.getItem(id)` method will tag items with a unique item ID using PDC.
- Menu classes can define a map of `itemid` → `MenuItemHandler`.
- The global menu listener will check for the `itemid` and dispatch to the correct handler.
- If no handler is found, fall back to classic `handleMenu()`.

---

## 1. LanguageManager Item Tagging

```java
ItemStack getItem(String id, ...) {
    ItemStack item = ...;
    ItemMeta meta = item.getItemMeta();
    meta.getPersistentDataContainer().set(
        new NamespacedKey("languagemanager", "itemid"),
        STRING,
        id
    );
    item.setItemMeta(meta);
    return item;
}
```

---

## 2. Define the `MenuItemHandler` Interface

```java
@FunctionalInterface
interface MenuItemHandler {
    void handle(Player player, ItemStack item, PersistentDataContainer data);
}
```

---

## 3. Menu Class Integration

```java
class ExampleMenu extends Menu {
    Map<String, MenuItemHandler> handlers = new HashMap<>();

    ExampleMenu(...) {
        handlers.put("General.Close", (player, item, data) -> player.closeInventory());
        handlers.put("General.Refresh", (player, item, data) -> this.open());
    }

    @Override
    Map<String, MenuItemHandler> getItemHandlers() {
        return handlers;
    }
}
```

---

## 4. Listener Logic (Centralized)

```java
@EventHandler
public void onInventoryClick(InventoryClickEvent event) {
    Player player = (Player) event.getWhoClicked();
    ItemStack item = event.getCurrentItem();
    if (item == null || !item.hasItemMeta()) return;

    Menu menu = menuSystem.getMenu(player);
    if (menu == null) return;

    String id = item.getItemMeta()
        .getPersistentDataContainer()
        .get(new NamespacedKey("languagemanager", "itemid"), STRING);

    MenuItemHandler handler = menu.getItemHandlers().get(id);
    if (handler != null) {
        handler.handle(player, item, item.getItemMeta().getPersistentDataContainer());
        event.setCancelled(true);
    } else {
        menu.handleMenu(event); // fallback
    }
}
```

---

## 5. In `setMenuItems()`

```java
ItemStack item = lgm.getItem("General.Close", ...);
inventory.setItem(49, item);
```

---

## Benefits

- ✅ Simple string-based dispatching (fast)
- ✅ No need for deep `ItemStack.equals(...)`
- ✅ Keeps `handleMenu()` for legacy use
- ✅ Modular, readable, and testable
- ✅ Minimal changes to existing system

---

## Optional

- Add debug logging for missing handlers
- Add validator to warn about duplicate or missing `itemid` tags in development
- Make a base class for menus with helpers like `registerHandler(String, MenuItemHandler)`
