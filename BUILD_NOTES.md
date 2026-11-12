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
