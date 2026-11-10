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

## Build status
- Source change committed to `patch/MainActivity.kt`.
- GitHub Actions builds the APK automatically on push to `main`.
