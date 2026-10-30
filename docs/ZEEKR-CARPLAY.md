# Zeekr CarPlay — Final Alpha 03

Package `com.openzeekr.carplay`, version code 45. Update an existing installation with this package in place to preserve settings.

See [README](../README.md) / [English](../README.en.md) for installation, tested configuration, fullscreen controls and screenshots. Source base: DiPlay commit `615b32ce0b40cd0b99ce94ba23a512001553684f`.

The fork gates presentation policy by application identity. Original receiver and OEM implementations remain in source. Unsupported BYD controls are hidden. Upstream update scheduling and update UI are disabled. USB, Wi-Fi Direct and audio implementations remain available. The setup guide requires explicit mode selection.

The owner-tested configuration is a Chinese pre-facelift Zeekr 001 (2023), OS 5.6 / Android 9 with Wi-Fi Direct and factory Bluetooth audio. Other vehicles/firmware and calls/Siri/navigation prompts require separate verification.

Overview and Display share saved bar-visibility values. Fresh installations show bars by default and existing choices are preserved. Hiding is requested through Android window APIs; the firmware-owned climate panel may remain visible. Bar appearance follows the Android system theme independently of app and CarPlay themes. No global car-service settings are modified.

Build using [BUILD.md](BUILD.md), JDK 25, Android SDK 37 and NDK 28.2.13676358. Standalone builds require `DIPLAY_AUTH_ASSETS_DIR` pointing outside the source tree to `offline-mfi/certificate.p7b` and `offline-mfi/identity.pk8`. The owner supplied those existing runtime files as a release attachment, outside Git history. Android signing keys remain local. Runtime acceptance and distribution rights are separate from source-code licensing.

Run checks from [AGENTS.md](../AGENTS.md). Automated tests verify Android window properties and app UI; emulators do not reproduce Zeekr's proprietary SystemUI or factory Bluetooth hardware.

Preserve [LICENSE](../LICENSE), [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and component notices. Original DiPlay/xcertplay/DiAuto credits remain in the app. The launcher uses upstream CarPlay artwork; the OEM return button uses the owner's supplied Zeekr PNG. No code from the protected paid APK was added. This is not an official Zeekr or Apple product.
