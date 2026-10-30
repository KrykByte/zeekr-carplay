# Zeekr CarPlay - Final Alpha 02

Version code 43, package `com.openzeekr.carplay`. Updates Alpha 01 using the same local Android signer; uninstalling is unnecessary. Existing connection, Bluetooth audio, onboarding and bar-visibility choices are retained.

Changes:

- Replace the debug manifest's literal `DiPlay HUD Test` launcher label with `@string/app_name` (Zeekr CarPlay).
- Restore the upstream green CarPlay launcher icon. The OEM return button inside CarPlay uses the exact `docs/assets/zeekr-carplay-user-logo.png` supplied by the owner; no compositing or wordmark was added.
- Rewrite audio onboarding in Russian and English. Bluetooth is recommended for the owner-tested pre-facelift Zeekr 001 2023, retaining experimental wording and the unverified calls/Siri/navigation caveat. No audio mode is selected automatically.
- Put independent status/navigation bar switches on Settings Overview, using the same preferences as Display. The compact overview puts quick controls before category links. Explain that Off keeps factory controls visible, On requests hiding, and OEM firmware can retain the climate panel. Fresh Zeekr installations show bars by default; saved visibility choices are preserved.
- Set status/navigation window backgrounds and foreground-appearance flags from Android system night mode in both the application and video host. The previous host inherited a fixed dark window background even when the OEM system selected dark icons for day mode. System bar appearance is independent of the app theme and the day/night mode sent to CarPlay. Reapply on focus/resume/configuration changes and existing theme polling; no car service, root or global setting changes.

Android 9 regression checks cover opposite app/system themes and both CarPlay night-mode values. UI snapshots include compact Russian, wide English and RTL at 150% font scale. These tests verify Android window properties and app controls, not the rendering of Zeekr's proprietary SystemUI. The real-car light/dark contrast fix must be verified on the head unit; firmware that does not report system night mode can still behave differently.

Core Wi-Fi Direct, USB and audio behavior is unchanged. Build and runtime-input requirements remain as described in ZEEKR-ALPHA-01.md and BUILD.md. Source attribution and license notices are retained. This build is not an official Zeekr or Apple product.
