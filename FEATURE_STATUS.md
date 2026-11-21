# DOClient v0.6 feature status

## Implemented in source
- Windows Electron launcher baseline: Microsoft device-code auth integration, official Mojang version metadata, isolated instances, Mojang Java runtime downloads with SHA-1 checks, Fabric profile adapter and modern Forge installer adapter.
- Purple/cyan theme with optional light theme.
- Content manager for per-instance mods, resourcepacks, shaderpacks and saves folders.
- About page: FOUNDER-XYnOX_, Discord invite, Zalith v1 inspiration note and independent-project disclaimer.
- Bounded logs, 15-minute version-manifest cache, visibility-aware chat polling, no-overlap polling guard, and lower-cost avatar dragging.
- Restrictive renderer Content Security Policy and navigation restrictions.

## Inspired by Zalith Launcher v1
Zalith v1 publicly describes improved version management, renderer support and content management. DOClient adopts high-level usability ideas only. It does not copy Zalith code or assets. The archived repository is GPL-3.0; any future source reuse must comply with the license. See the DOClient README.

## Explicitly incomplete / external work
- Android shell is not a Java Edition runtime. No Android Java runtime, Pojav/GLFW renderer integration, game asset pipeline, touch/gamepad input bridge or native launch adapter is included. Physical ARM64 device testing is required.
- Social relay is a development prototype: in-memory, not end-to-end encrypted, no verified account identity, persistent database, staffed moderation or production abuse controls. Run locally only.
- Skin/cape files are local previews only. Account publishing is not implemented.
- Microsoft device-code auth requires an eligible Java Edition account and end-to-end testing. Public distribution may require a developer-owned Microsoft app registration/review.
- Full game install/launch, Fabric/Forge end-to-end tests, Windows installer signing and Android APK build/device testing were not run in this environment.
- The existing Minecraft launch library may not provide reliable cancellation for every internal bootstrap download.

## Commands
- `npm install`
- `npm run check`
- `npm test`
- `npm run social:dev`
- `npm start`
- `npm run pack:win`
