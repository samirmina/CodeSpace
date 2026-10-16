# Plan: Fix `storeSessions` Flag to Suppress In-Memory Session Tracking

## 1. Issue Summary

**What it does:** When a task is created with the "Do not store sessions" checkbox checked, the timer still increments the session counter in memory and records the completed session. The session is only filtered out when writing to disk, but the in-memory state shows the session as stored/counted.

**What it should do:** The `storeSessions == false` flag must suppress **both** in-memory session incrementing **and** disk persistence. The timer should run normally, but no session data should be recorded anywhere.

**Engineering discrepancy:** The `storeSessions` flag is checked in `JsonStorage.save_user_data()` (disk layer) but **not** in `PomodoroView.complete()` (timer layer). This creates a two-tier system where the flag only affects persistence, not state — violating the principle that a single flag should have consistent semantic meaning across all layers.

---

## 2. Root Cause Analysis

### Call chain on timer completion

```
PomodoroView.complete()  (line 454)
  → ps.increment_session(current.get_id())     // in-memory counter ALWAYS incremented
  → storage.save_user_data(userId, ts, ps, settingsService)  // disk write filtered by storeSessions
```

### Where the guard exists (correct)
- `JsonStorage.save_user_data()` line 125: `if (task != null && task.is_store_sessions()) { userPomos.add(p); }`
- `SessionsView.is_pomodoro_visible()`: filters ephemeral tasks from UI
- `ReportView.gather_data()`: filters ephemeral tasks from stats

### Where the guard is MISSING (bug)
- `PomodoroView.complete()` line 454: `ps.increment_session()` is called **unconditionally**, regardless of `storeSessions`

### Consequence
1. User creates task with "Do not store sessions" checked
2. User runs timer to completion
3. `increment_session()` runs → `completed_sessions` becomes 1
4. `save_user_data()` runs → pomodoro is filtered out → disk is clean
5. User sees session count = 1 in PomodoroView overview (in-memory)
6. User perceives this as "sessions are stored" despite the flag

---

## 3. Fix Plan

### File: `src/jfx/PomodoroView.java`

**Location:** Line 454, inside `complete()` method

**Current code:**
```java
if (current != null) { ps.increment_session(current.get_id()); if (currentUser != null) storage.save_user_data(currentUser.get_id(), ts, ps, settingsService); }
```

**Replace with:**
```java
if (current != null) {
    boolean storeSession = ts.try_get_task(current.get_task_id())
        .map(t -> t.is_store_sessions())
        .orElse(true);
    if (storeSession) {
        ps.increment_session(current.get_id());
    }
    if (currentUser != null && storeSession) {
        storage.save_user_data(currentUser.get_id(), ts, ps, settingsService);
    }
}
```

**Behavior after fix:**
| Task type | `increment_session()` | `save_user_data()` | Timer UI | SessionsView | Disk |
|-----------|----------------------|-------------------|----------|--------------|------|
| Normal (`storeSessions=true`) | ✅ Called | ✅ Called | Normal | Visible | Written |
| Ephemeral (`storeSessions=false`) | ❌ Skipped | ❌ Skipped | Normal | Hidden | Empty |

---

## 4. Additional Issues Found During Investigation

### 4.1 `storeSessions` semantics are inconsistent across the codebase

| Layer | Checks `storeSessions`? | Correct? |
|-------|------------------------|----------|
| `JsonStorage.save_user_data()` | ✅ Yes | ✅ Correct |
| `SessionsView.refresh()` | ✅ Yes | ✅ Correct |
| `SessionsView.updateOverview()` | ✅ Yes | ✅ Correct |
| `SessionsView.drawMostProductive()` | ✅ Yes | ✅ Correct |
| `ReportView.gather_data()` | ✅ Yes | ✅ Correct |
| `PomodoroView.complete()` | ❌ No | ❌ **BUG** |
| `PomodoroView.refresh_sessions()` | ❌ No | ⚠️ Minor (reads from filtered pomodoros) |
| `PomodoroView.update_overview()` | ❌ No | ⚠️ Minor (reads from filtered pomodoros) |

### 4.2 `PomodoroView.refresh_sessions()` and `update_overview()` do not filter

These methods read from `ps.get_total_sessions()` and `ps.get_total_focus_time_minutes()`, which return **all** pomodoros including ephemeral ones. However, since `complete()` will no longer increment sessions for ephemeral tasks, these methods will naturally show correct values. No additional fix needed.

### 4.3 `SessionsView.showSessionDetails()` filters correctly

Already fixed in previous work. `is_pomodoro_visible()` is called before displaying session details.

---

## 5. Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Timer completion for ephemeral task shows 0 sessions | Low | Expected behavior — user explicitly chose not to store |
| `ts.try_get_task()` returns empty Optional | Very Low | `.orElse(true)` defaults to storing — safe fallback |
| Existing saved data for ephemeral tasks | None | `save_user_data()` already filters on disk; old pomodoros in memory are harmless |
| Notification popup for ephemeral task | None | Popup still shows; "Start Next Session" will create new pomodoro |

---

## 6. Files to Modify

| File | Line | Change |
|------|------|--------|
| `src/jfx/PomodoroView.java` | 454 | Guard `increment_session()` and `save_user_data()` with `storeSessions` check |

---

## 7. Verification Steps

1. Create task with "Do not store sessions" checked
2. Run timer to completion
3. Verify PomodoroView overview shows 0 sessions for that task
4. Verify SessionsView does not show the session
5. Verify ReportView does not include the session
6. Verify `data_<userId>.json` has empty `pomodoros` array
7. Restart app, verify no session persisted
8. Create normal task without checkbox, verify normal behavior unchanged

---

## 8. Test Impact

- No existing unit tests cover `PomodoroView.complete()` (UI layer)
- No model/service tests need changes
- All 156 existing tests should continue passing
- Consider adding integration test for `storeSessions` flag behavior
