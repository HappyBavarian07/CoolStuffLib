# Anticheat Menu Refactor Roadmap (2025-09-20)

## Milestones & Tasks

### Design
- **API Design** (M, high): Design new button registration and callback APIs for Menu and MenuAddonManager.
- **Persistence Design** (S, high): Specify AddonButtonSpec format and persistence mechanism.

### Implementation
- **Menu API Implementation** (L, high): Implement registerButton, tryRegisterButton, forbiddenSlots, and slotActions in Menu.
- **MenuListener Routing** (M, high): Update MenuListener to route clicks to registered actions.
- **MenuAddonManager Extensions** (L, high): Add AddonButtonSpec, global/instance registration, persistence, handlerId mapping.

### Persistence
- **Serialization/Deserialization** (M, medium): Implement JSON serialization for AddonButtonSpec and ItemStack.
- **Persistence Loader** (S, medium): Implement loader to read/write addon_buttons.json.

### Integration
- **Addon Integration** (M, high): Update add-ons to use new registration and handlerId APIs.
- **Migration** (M, high): Migrate persistent button specs and verify legacy compatibility.

### Tests
- **Unit Tests** (M, high): Write unit tests for registration, slot finding, forbiddenSlots, click routing.
- **Integration Tests** (M, high): Test persistence, handlerId rebinding, and menu recreation.

### Documentation
- **API Documentation** (S, medium): Document new APIs and migration steps.

---

## Progress Tracking Schema
- milestone: string
- taskId: string
- done: boolean

---

## Migration & Verification Checklist
- [ ] Implement and deploy new APIs.
- [ ] Update add-ons to use new registration.
- [ ] Migrate and load persistent button specs.
- [ ] Run all unit and integration tests.
- [ ] Verify legacy menus and new buttons work together.
- [ ] Document all changes and update usage guides.

---

## Example Save Command
```
docker run --rm -i -v mcp-memory-data:/data -v "$(pwd)":/local-directory mcp/memory write /projects/adminpanel/anticheat_menu/anticheat_menu_roadmap.md < anticheat_menu_roadmap.md
```
Or:
```
mcp-memory write /projects/adminpanel/anticheat_menu/anticheat_menu_roadmap.md < anticheat_menu_roadmap.md
```
(Adapt the path and command to your environment as needed.)

