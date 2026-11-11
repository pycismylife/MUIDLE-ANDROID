# MU IDLE APK – Build Notes

## Clean rebuild scope

This rebuild intentionally removes the previously added custom tools and starts with one feature only: Auto Move Map.

The Android WebView shell and the original game page are retained. The following older custom features are excluded from this build: Auto Filter, Auto Stats, Auto Reset, Drop Table, monster hiding, minimap hiding, FPS limiter, offline speed controls, and other diagnostic/graphics controls.

## Auto Move Map – first implementation

- Configure a level threshold (1–400), Map Y, and Map Z.
- Farm Map Y from Lv 1 through the configured threshold.
- Switch to Map Z when the character level exceeds the threshold.
- When the Reset counter increases, return to Map Y.
- Check level and Reset state once per second.
- Use the game's own map list and map row click path; do not invent map IDs or send a custom server request.
- Read map IDs, names, and lock state from the live map list.
- Refuse to click a map marked locked.
- Save the map choices, threshold, and enabled state locally on the device.
- Do not add in-map movement or monster/spot controls; the game handles combat and movement within the selected map.
- Status text reports data-read failures, locked maps, and whether the selected map is confirmed as current by the game's map-list UI.

## Validation

A successful Android build confirms compilation only. Live in-game testing is still required to verify the actual map list selectors, map transition timing, level threshold behavior, and Reset return behavior.


## Inventory Diagnostic APK

- Adds an on-screen read-only diagnostic panel, separate from Auto Move Map.
- Scan visible inventory/modal DOM elements and buttons; capture one user-selected item click and inspect an open item-detail popup.
- Copy a JSON report to Android clipboard for sharing in ChatGPT.
- No sell, equip, move, or inventory mutation actions are implemented in Diagnostic mode.
- A successful build confirms compilation only; actual DOM evidence must be collected from the live game.
