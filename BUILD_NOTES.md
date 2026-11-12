# MU IDLE APK – Clean module rebuild

## Scope

This branch rewrites only the two custom modules in `patch/MainActivity.kt`:
- Auto Move Map
- Auto Filter

The Android WebView shell and original game page remain unchanged. The separate Diagnostic module and its obsolete imports are removed. No unrelated game behavior, FPS controls, or graphics settings are added or modified.

## Auto Move Map

- Level 1 through configured threshold X: Map Y.
- Level above X: Map Z.
- When Reset increases: return to Map Y.
- Poll game level and Reset state once per second.
- Read map IDs, names, and lock state from the live game map list.
- Refuse to click a map marked locked.
- Confirm the selected map from the game's current-map row; avoid repeated clicks.
- Save threshold, map choices, and enabled state locally on the device.

## Auto Filter

- Integrated directly below Auto Move Map in the same Settings panel; no separate floating launcher.
- Shared scope: equipment type, class, minimum Option, minimum Upgrade, Luck, and Bonus.
- Supported equipment groups: Weapon, Shield, Headgear (Helm + Circlet), Armor, Pants, Gloves, Boots.
- Each Rule contains 1–6 EX options combined with AND; multiple Rules are combined with OR.
- An item matching at least one Rule is kept. An in-scope item matching no Rule is a sell candidate.
- Equipped items and unrecognized items are always kept.
- Auto selling handles one item at a time and revalidates its inventory ID and equipped state before the game's two-step sell confirmation.
- Rules, shared filters, and enabled state are saved locally.
- An empty Rule set cannot enable automatic selling.

## Validation

The GitHub Actions build validates the injected JavaScript syntax with Node before compiling the Android APK. A successful Android build confirms compilation only. Live in-game testing is still required to verify game DOM selectors, map transition timing, inventory item identity, and the sell confirmation flow.


## Approved UI design standard (2026-11-12)

Use this visual and interaction pattern as the default for future MU IDLE Manager features unless the user explicitly approves a different design:
- Preserve the existing dark brown / antique-gold theme, compact spacing, readable labels, and mobile-first layout.
- Settings features belong in the shared Settings panel; do not create separate floating launchers unless explicitly requested.
- Auto Move Map uses exactly two compact rows:
  - `Lv 1 - [threshold X input] [Map Y selector]`
  - `Lv [X+1] - [reset-required level read live from game] [Map Z selector]`
  - The reset-required level is system/game-derived and read dynamically; never ask the user to enter it or invent a formula.
  - When Settings opens, open the game's map list, wait until its rows are actually available, read them, then close the map list. Show success only after data is read; provide a reload control for failure/retry.
- Auto Filter keeps Class and Equipment Type selectors independently inside every Rule, directly below that Rule's heading. Each Rule's EX options remain within the same card.
- Auto Filter semantics: AND within a Rule, OR between Rules, max 6 EX options per Rule. Keep common Option / Upgrade / Luck / Bonus filters separate from per-Rule Class and Equipment Type.
- Keep these approved layouts as the reference pattern for subsequent feature development. Do not alter existing automation logic or add unrequested behavior while implementing visual changes.
