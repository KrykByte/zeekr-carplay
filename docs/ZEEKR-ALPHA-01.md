# Zeekr CarPlay - Final Alpha 01

Community adaptation of DiPlay at commit 615b32ce0b40cd0b99ce94ba23a512001553684f. Original receiver and vehicle implementations are retained; ZeekrProfile gates the fork UI by application identity. The package is com.openzeekr.carplay and installs separately from the previous test package; the local debug signer is retained. Version code: 42.

Changes: English/Russian application resources and language picker, explicit transport and audio onboarding, car hotspot marked experimental, Wi-Fi Direct recommendation limited to the owner-tested Chinese pre-facelift Zeekr 001 2023 / OS 5.6 / API 28 / iOS 27.2 configuration, optional automatic connection, reduced settings, hidden BYD controls, replacement application and OEM-return branding. The in-CarPlay settings gesture opens the same application settings. Core USB, Wi-Fi Direct, audio and OEM code is retained. Existing user-selected transport/audio preferences are not reset by migration; first-run confirmation is required for this alpha guide.

Upstream automatic update scheduling is cancelled and its worker exits immediately for the fork. UI update controls and cached banners are hidden. There is no configured fork update server.

For a new installation, transport and audio must be chosen before completing the guide. Bluetooth mode offers no CarPlay audio to the phone and is experimental; factory metadata/artwork depend on the car. Calls, Siri and navigation must be validated separately. This is an alpha, not official Zeekr or Apple software.

Source/credits: https://github.com/shihabal3amri/DiPlay and https://github.com/shilapi/xcertplay. Preserve LICENSE and docs/THIRD_PARTY_NOTICES.md, including AGPL-3.0 UI and component notices. No code from the paid protected CarPlay APK was added.

Logo: Zeekr logo SVG credited to Zeekr; downloaded from https://upload.wikimedia.org/wikipedia/commons/1/1e/Zeekr_logo.svg, described at https://commons.wikimedia.org/wiki/File:Zeekr_logo.svg . The app icon and OEM return image compose the emblem and wordmark on a square background. The mark identifies the target vehicle; no endorsement is implied.

Build as in docs/BUILD.md using JDK 25, Android SDK 37 and NDK 28.2.13676358. Runtime accessory credentials are supplied explicitly from outside the source repository using DIPLAY_AUTH_ASSETS_DIR. The included APK for the owner uses the same runtime inputs as the previous tested build. Those inputs and Android signing private key are excluded from the source archive. Do not assume the source code licenses grant rights to distribute every runtime asset; public distribution has not been performed.
