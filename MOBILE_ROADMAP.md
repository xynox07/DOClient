# DOClient Mobile roadmap

## v0.7 — Android-first shell (done)
- [x] Logo, app icon, local profile persistence, restricted WebView navigation.

## v0.8 — Pending tools + new UI (this build, source only, not compiled/tested on a device)
- [x] New pixel-glass UI: 3D character on Home, account chip, PLAY, Instances / Mods / Resource packs tiles, servers list, accent themes, calm mode.
- [x] Offline accounts with real Minecraft offline UUIDs.
- [x] Instances with version, loader, RAM and a real `options.txt` performance preset (Potato / Balanced / Quality).
- [x] Mojang release-version list (15-minute cache, offline fallback).
- [x] Import mods (.jar), resource packs and shaders (.zip) per instance; list and remove.
- [x] Modrinth search + install with host allow-list and SHA-512 verification.
- [x] Skin and cape preview rendered in 3D (local files, validated PNG sizes).
- [x] PLAY runs an honest preflight (account, instance, profile, runtime) and reports the missing engine.
- [ ] Android Studio build + install verification.
- [ ] Physical device layout, file-picker and Modrinth download testing.

## v0.9 — Native runtime and engine
- [ ] Select an Android Java runtime/launcher engine with compatible licensing and active maintenance.
- [ ] ARM64 native dependencies, renderer/window bridge, touch input.
- [ ] Version/asset/library installer into the instance folders.
- [ ] Microsoft OAuth (premium accounts) with encrypted token storage.
- [ ] Game process lifecycle, logs, crash recovery; Fabric first, Forge/NeoForge after verification.

## Deferred
- Windows/PC client, public friends/chat, and publishing skins/capes to Mojang.
