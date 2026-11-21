# DOClient Mobile v0.8

**Android-first project. PC/Windows work is intentionally deferred.**

DOClient uses the supplied DOClient logo and a dark purple/cyan mobile interface. The Android project is a native Kotlin Activity hosting a local mobile UI, with Android document-picker support for local PNG previews and a Discord deep link.

## v0.8 added (source only; not compiled or device-tested)
- New UI: 3D character home screen, PLAY with honest preflight, Instances/Mods/Resource packs tiles, servers list, accent themes.
- Offline accounts (real offline UUIDs), instances with loader + `options.txt` presets.
- Mojang version list, local mod/pack/shader import, Modrinth search + SHA-512-verified install.
- 3D skin and cape preview.

## v0.7 implemented
- User-provided app logo installed as Android launcher icon (legacy densities + adaptive icon) and in-app brand asset.
- Mobile-first home, profiles, skin studio, settings and About screens.
- About credits **FOUNDER-XYnOX_** and links to https://discord.gg/5Bc4a822M.
- Local profile list persisted on-device (these are UI profiles, not yet Minecraft game instances).
- Native Android PNG picker for local skin/cape previews.
- External Discord links open outside the WebView.
- Local-only WebView UI; external navigation is blocked except the approved Discord invite.
- Responsive layout, touch-friendly controls, safe-area insets and reduced-motion support.
- Android version metadata updated to 0.7.0.

## Open the Android project
1. Install Android Studio and a compatible JDK 17.
2. Open the `android/` folder.
3. Allow Android Studio to sync the Gradle project and install SDK 35 / build tools if prompted.
4. Build with Android Studio's **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

The environment used to prepare this source did not have Android Studio/SDK device tooling available, so no APK build or physical device test is claimed.

## Important: Minecraft Java engine is still pending
This v0.7 is an Android app shell and launcher UI, **not yet a working Minecraft Java launcher**. It does not bundle or integrate:
- a supported Android Java runtime and architecture-specific native libraries;
- a Minecraft-compatible OpenGL/Vulkan renderer and window/input bridge;
- the official game asset/version installation pipeline for Android storage;
- Microsoft OAuth callback and secure token storage in Android;
- native game process lifecycle, crash recovery, touch controls or gamepad mapping.

The app intentionally does not show a fake Play button or claim to launch Minecraft. Completing these features requires selecting a compatible Android Java launcher/runtime stack, checking its license and redistribution terms, integrating it with the app's storage/auth lifecycle, and testing on real ARM64 phones/tablets (including GPU, RAM, thermal and controller coverage).

## Current platform scope
- Android: active target.
- Windows/PC: retained in the source tree for later; not being developed in this milestone.
- Friends/chat: omitted from this mobile build until a secure hosted backend and moderation plan exist.
- Skin/cape: local PNG preview only; no account upload.

## Zalith Launcher v1 inspiration
DOClient is independently implemented and takes inspiration only from high-level launcher usability concepts. It does not copy Zalith code or artwork and is not affiliated with ZalithLauncher, Mojang or Microsoft. The archived Zalith repository is GPL-3.0; any future source reuse must comply with that license and its notice/source obligations.
