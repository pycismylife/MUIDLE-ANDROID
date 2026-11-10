# MU IDLE APK – Build Notes

## Auto Filter PRO – Read-only Dry Run (2026-11-10)

- Added an independent Auto Filter PRO panel to the Android WebView control panel.
- Reads the current character inventory from `window.S.inv` and equipped items from `window.S.eq`.
- Supports seven equipment groups only: Weapon, Shield, Headgear (Helm + Circlet), Armor, Pants, Gloves, Boots.
- Wings and Jewels are excluded from the filter.
- Each Keep rule applies to exactly one equipment group and can combine conditions:
  - Minimum Excellent line count
  - Luck: any / required / absent
  - Minimum upgrade level (+)
  - Minimum Option
  - Match current character class
- Conditions inside one rule use AND; separate Keep rules use OR.
- Excellent items matching at least one Keep rule are marked Keep.
- Recognized Excellent items matching no Keep rule are marked Eligible to Sell (simulation only).
- Equipped items and unrecognized item groups are protected.
- Non-Excellent items are ignored.
- Rules are saved locally on the device.
- Scan is manual and read-only. No sell request is sent; no inventory item is changed.
- Auto Map PRO remains a disabled placeholder.
- Actual automatic selling is NOT implemented. It remains blocked until server-side sell acknowledgement and inventory refresh behavior are verified.


## Auto Map PRO – Two-stage map automation (2026-11-10)

- Added an independent Auto Map PRO configuration panel to the Android WebView overlay.
- Reads map IDs, names, and locked/unlocked state from the game's existing Di chuyển (M) map list UI; does not hardcode map names or IDs.
- Provides Map 1 (Lv 1 through configured threshold) and Map 2 (above threshold through Reset), with default threshold Lv 300 (configurable 1–400).
- Saves both map selections, threshold, and enabled state in localStorage.
- When enabled, checks character level every 2 seconds and requests a map change only when the active stage changes. After Reset returns the character to Lv 1, it switches back to the Map 1 stage.
- Checks the selected map's current unlock state before clicking it; if locked or unavailable, reports status and does not switch.
- Uses the game's existing map-selection UI click path. Does not toggle or modify native "Tự đổi lên bãi mới" (zoneAuto) setting.
- Initial implementation requires live in-game testing for map list timing, level transitions, and Reset behavior; it does not claim server-side verification of successful map change.

## Build status
- Source change committed to `patch/MainActivity.kt`.
- GitHub Actions builds the APK automatically on push to `main`.
