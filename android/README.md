# DOClient Android v0.8

Android-first mobile UI project. Open this folder in Android Studio (JDK 17, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, SDK 35).

## Build
Use Android Studio **Build > Build Bundle(s) / APK(s) > Build APK(s)** after Gradle sync. The source package includes the user-provided logo as launcher icons and in-app branding.

## Implemented
- Kotlin Activity + local WebView UI.
- Responsive mobile screens for home, local profile list, skin studio, settings and About.
- Native Android document picker for local PNG files.
- Discord invite opened via Android external intent.
- Local app navigation and profile persistence.

## Not yet a Minecraft Java launcher
No Android Java runtime, game renderer, native launch bridge, Minecraft asset installer, Microsoft OAuth integration, or game input system is included. A compatible runtime stack must be selected and license-reviewed before implementation. Real ARM64 device testing is mandatory. Do not advertise this shell as capable of launching Minecraft Java.

No APK was built or tested in the generation environment.
