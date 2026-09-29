# Minimal Launcher V2 — Samsung S23 Ultra

A deliberately sparse black-and-white Android launcher intended to reduce phone use.

## Home screen

Default home apps:
- Phone
- Messages
- Maps
- Camera
- GPay
- Authenticator

Maximum home apps: 7.

Long-press **All Apps** to select/deselect home-screen apps.

## App drawer

Swipe up or tap **All Apps →**.

The drawer has:
- Search
- All installed launchable apps
- Swipe down to return to Home
- Launcher settings

## Distraction delay

The architecture includes a per-app delay list. The UI is intentionally conservative in V2; the next revision can expose a simple "Delay distracting apps" settings page with 5/10/20 second choices.

## Samsung package names

Defaults are aimed at Samsung One UI:
- Phone: com.samsung.android.dialer
- Messages: com.samsung.android.messaging
- Camera: com.sec.android.app.camera
- Google Maps: com.google.android.apps.maps
- GPay: com.google.android.apps.nbu.paisa.user
- Google Authenticator: com.google.android.apps.authenticator2

If your installed version uses another package, select the app from Launcher settings.

## Build

Open the project in Android Studio.
Use JDK 17 and Android SDK 35.
Sync Gradle and run the `app`.

Then:
Settings → Apps → Choose default apps → Home app → Minimal Launcher

Menu wording varies by One UI version.

## Design philosophy

No icons, widgets, feeds, wallpapers, badges or colorful UI.
The goal is to make opening an app an intentional action rather than a visual trigger.
