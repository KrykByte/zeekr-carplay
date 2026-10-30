# Zeekr CarPlay

Independent community adaptation of DiPlay for the Chinese pre-facelift **Zeekr 001 (2023), Zeekr OS 5.6 / Android 9**. Current version: **Final Alpha 03**, Android package `com.openzeekr.carplay`.

This repository starts with the source snapshot based on the owner's tested Alpha 02, with contact/project links added in Alpha 03. It is intended to remain private while distribution questions are reviewed. It is not an official Zeekr or Apple product.

## Working configuration / Проверенная конфигурация

The owner reported stable operation using **Wi-Fi Direct + factory Bluetooth audio**, including steering-wheel track controls and reconnection after a head-unit restart. After Alpha 02, the owner reported that the interface fixes also worked without problems. These are owner reports for this configuration, not certification of other cars, firmware versions or all CarPlay functions.

Владелец проверил китайский Zeekr 001 дорестайлинг 2023, OS 5.6 / Android 9. Рекомендуемая проверенная конфигурация: **Wi-Fi Direct и звук через штатный Bluetooth**. Точка доступа автомобиля остаётся экспериментальной. Интеграция навигации с приборной панелью не заявлена; звонки, Siri и навигационные подсказки требуют отдельной проверки.

Contact: [Telegram @AibatK](https://t.me/AibatK).

## Changes

- Russian/English setup guide with explicit connection and audio choices.
- Wi-Fi Direct recommendation limited to the tested vehicle configuration.
- Bluetooth audio recommendation, retaining experimental status.
- Simplified settings; unsupported BYD controls hidden while implementations remain in source.
- Correct launcher name, separate CarPlay application icon and Zeekr return-button image.
- Quick access to system-bar visibility and bar contrast following the Android system theme.
- Upstream automatic update checks disabled.

Details: [Alpha 01](docs/ZEEKR-ALPHA-01.md), [Alpha 02](docs/ZEEKR-ALPHA-02.md), [Alpha 03](docs/ZEEKR-ALPHA-03.md).

## Upstream and credits

- [DiPlay by shihabal3amri](https://github.com/shihabal3amri/DiPlay), source base: `615b32ce0b40cd0b99ce94ba23a512001553684f` (0.2.22).
- [xcertplay by shilapi](https://github.com/shilapi/xcertplay), the underlying receiver.
- [DiAuto](https://github.com/shihabal3amri/DiAuto), credited for adapted UI.

The original DiPlay README is retained in [docs/DIPLAY-README.md](docs/DIPLAY-README.md); xcertplay documentation is in [docs/UPSTREAM-README.md](docs/UPSTREAM-README.md). Original authorship and component notices remain intact. No code from the protected paid APK was added.

## Build and verification

See [BUILD.md](docs/BUILD.md). Source-only builds contain **no CarPlay accessory certificate or private key**. A source-only APK therefore does not reproduce standalone authentication without separately provisioned runtime inputs. Ready-to-install APKs are provided in private [Releases](https://github.com/KrykByte/zeekr-carplay/releases), outside Git history. The owner also requested a separate private release archive of the existing CarPlay runtime files. The Android APK-signing key is not uploaded.

Alpha 02 validation completed: **3,543 tests passed, one skipped**, required lint and application builds succeeded. Tests cover Android 9 system-bar contrast and large-font UI; the owner's later report confirms successful operation on the target car.

Runtime credentials and APKs are excluded from Git history and stored only as private release attachments when explicitly requested by the owner. APK signing keys, local logs, diagnostic attachments and local toolchains are excluded from all uploads. The upstream Pages workflow is retained as documentation rather than an active workflow, so pushing this repository does not deploy its download site.

## Licenses and distribution

Retain [LICENSE](LICENSE), [THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md) and component notices. The receiver uses GPL-3.0; adapted UI carries AGPL-3.0 terms. Some images and other assets have separate terms; code licenses do not grant rights to trademarks or every runtime asset. Public binary distribution remains subject to review of those components and authentication inputs.
