# Changelog

## 1.0.0 (unreleased)
- Compose BOM pinned to 2026.06.01: newer Compose (1.12+) requires AGP 9 and compileSdk 37.
- Added material-icons-core (no longer pulled in transitively by Material 3).
- Start.io 5.3.1 integrated from code (not auto-started): free tier only, after an ads consent prompt (setUserConsent "pas"), no splash/return ads, test ads in debug only. Real banners will be placed next to real results.
- Project skeleton: Compose design system (light/dark, RTL/LTR), country/currency/language/theme settings.
- Strings in en, zh, hi, es, ar, fr.
- Supabase schema migration 0001.
- GitHub Actions: unit tests, debug build, signed release build with version guard.
