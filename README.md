# PriceScope (com.pricescope.app)

Global product price comparison for Android. Kotlin, Jetpack Compose.

## Status
Skeleton milestone: design system, country / currency / language / theme settings
(all ISO countries from system data), six-language strings, Supabase migration,
CI build with signing from GitHub Secrets.

Not implemented yet (each needs a real data source or credentials first):
price search results, Firebase Auth / FCM, Supabase client, Start.io ads,
Google Play Billing. The Home screen says so openly instead of showing fake prices.

## Build from the phone (GitHub Actions)
1. Actions > **Build** > Run workflow (leave "release" off) for a debug APK + unit tests.
2. For a signed release, add these repository secrets first
   (Settings > Secrets and variables > Actions):
   `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
   `KEYSTORE_BASE64` is your EXISTING keystore file encoded in Base64.
   Never commit the keystore. The build prints the SHA-1 / SHA-256 of the key
   in the run summary; add them in Firebase project settings.
3. Run **Build** with "release" on: produces signed APK + AAB, verifies the signature,
   and tags `v<VERSION_NAME>`.

## Versions
Edit `version.properties` before every release. `VERSION_CODE` must be higher than
the previous release; the CI refuses reused or lower codes and existing tags.

## Database
`supabase/migrations/0001_pricescope_schema.sql`. Add future changes as new numbered files.
Never edit a migration that has already been run.
