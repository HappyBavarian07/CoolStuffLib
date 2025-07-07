# Generalized Cache System Roadmap for CoolStuffLib

## Overview
This document outlines a detailed roadmap for designing and implementing a generalized cache system for the CoolStuffLib project. The system will support both in-memory and persistent caching, provide a unified interface, and include a specialized implementation for the Expression Engine. The roadmap also includes an advanced AI prompt for automated implementation and progress tracking.

---

## Roadmap

### 1. Requirements Analysis
- Identify all library components that would benefit from caching (e.g., Expression Engine, configuration, language management).
- Define use cases for both in-memory and persistent caching.
- Determine thread-safety and performance requirements.

### 2. Package Structure
- Create a new package: `de.happybavarian07.coolstufflib.cache`.
- For expression-specific caching, create a subpackage: `de.happybavarian07.coolstufflib.cache.expression`.

### 3. Core Cache Interface
- Define a generic `Cache<K, V>` interface with methods:
  - `V get(K key)`
  - `void put(K key, V value)`
  - `void remove(K key)`
  - `void clear()`
  - `boolean containsKey(K key)`
- Document thread-safety expectations.

### 4. In-Memory Cache Implementation
- Implement `InMemoryCache<K, V>` using `ConcurrentHashMap`.
- Ensure thread-safety and high performance.
- Add optional size limit and LRU eviction policy.

### 5. Persistent Cache Implementation (Stub/Pluggable)
- Define `PersistentCache<K, V>` interface extending `Cache<K, V>`.
- Provide a stub implementation (e.g., file-based, database-ready, or serialization-based).
- Document extension points for future persistent backends.

### 6. Unified Cache Management
- Provide a `CacheManager` to register, retrieve, and manage different cache instances.
- Allow switching between in-memory and persistent caches via configuration.

### 7. Expression Engine Cache Integration
- In `de.happybavarian07.coolstufflib.cache.expression`, implement:
  - `ExpressionCacheKey` (combines expression string and relevant variable state hash).
  - `ExpressionCache` (uses `Cache<ExpressionCacheKey, V>` for parsed ASTs and evaluation results).
  - Utility to extract relevant variables from an expression (AST analysis).
- Refactor the Expression Engine to use the new cache system for:
  - Parsed expressions (ASTs).
  - Evaluation results (expression + variable state).

### 8. API and Configuration
- Expose cache configuration and management via public API.
- Allow users to select cache type and configure size/eviction policies.

### 9. Documentation
- Document the cache system, usage examples, and integration points.
- Provide migration notes for existing users.

### 10. Testing and Validation
- Add unit and integration tests for all cache implementations.
- Validate performance and correctness under concurrent access.
- Test Expression Engine integration and cache hit/miss scenarios.

### 11. Maintenance and Extension
- Plan for future persistent backends (e.g., Redis, database, disk).
- Monitor and optimize cache performance as needed.

---

## Advanced AI Implementation Prompt

You are an AI development assistant tasked with implementing the above cache system for CoolStuffLib. Follow these instructions:

1. **Analyze the Current State:**
   - Inspect the project structure and existing code to determine which roadmap steps are already completed.
   - Mark off completed steps in the roadmap.
   - Identify missing or incomplete components.

2. **Iterative Implementation:**
   - For each uncompleted roadmap step, implement the required code in the appropriate package and file.
   - After each implementation step, re-analyze the project to update the roadmap status.
   - Ensure all code follows project conventions and is free of unnecessary comments.

3. **Interface and Implementation:**
   - Start with the generic `Cache<K, V>` interface.
   - Implement `InMemoryCache<K, V>` and a stub for `PersistentCache<K, V>`.
   - Add a `CacheManager` for unified cache management.

4. **Expression Engine Integration:**
   - Implement `ExpressionCacheKey` and `ExpressionCache` in the expression subpackage.
   - Refactor the Expression Engine to use the new cache system for parsing and evaluation.

5. **Testing and Documentation:**
   - Add tests for all cache implementations and integration points.
   - Document the cache system and provide usage examples.

6. **Progress Tracking:**
   - Each time the AI starts, it must:
     - Analyze the current state of the codebase.
     - Mark off completed roadmap steps.
     - Clearly indicate which steps remain.
     - Proceed with the next logical step.

7. **Best Practices:**
   - Ensure thread-safety, performance, and clean code.
   - Avoid unnecessary comments; use Javadoc only where required by project standards.
   - Follow all project-specific coding and documentation rules.

---

## Example Usage

```
Cache<String, Object> cache = new InMemoryCache<>();
cache.put("key", new Object());
Object value = cache.get("key");
```

---

## Status Tracking Table

| Step | Description | Status |
|------|-------------|--------|
| 1    | Requirements Analysis | ✅ Complete |
| 2    | Package Structure | ✅ Complete |
| 3    | Core Cache Interface | ✅ Complete |
| 4    | In-Memory Cache Implementation | ✅ Complete |
| 5    | Persistent Cache Implementation | ✅ Complete |
| 6    | Unified Cache Management | ✅ Complete |
| 7    | Expression Engine Cache Integration | ✅ Complete |
| 8    | API and Configuration | ✅ Complete |
| 9    | Documentation | ✅ Complete |
| 10   | Testing and Validation | ✅ Complete |
| 11   | Maintenance and Extension | ✅ Complete |

---

This roadmap and prompt are designed to ensure a robust, extensible, and maintainable cache system for CoolStuffLib, with clear progress tracking and AI-driven implementation support.
