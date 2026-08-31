# VoLTE Checker — Check Volte

> Kiểm tra VoLTE / IMS, band & quốc gia, và mở khóa VoLTE ẩn — với Shizuku đặc quyền khi cần.

**Repo:** https://github.com/devprpvip/test-volte

## Tính năng
- **Chẩn đoán VoLTE:** `TelephonyManager` + `CarrierConfig` + `ImsMmTelManager` (ổn định, multi-SIM, Android 14-15)
- **Band & Quốc gia:** `CellInfoLte.getEarfcn()` → 3GPP B3/B1/B8/n78 + `MCC 452=VN` + `CarrierBandDatabase` (VN/US/JP/KR/DE/IN/GB/TH)
- **Fix all dòng máy:** Xiaomi (`*#*#86583#*#*`), Pixel (Shizuku), Samsung (`*#0011#`/`*#2263#`), Oppo (`*#800#`), Sony (`*#*#4636#*#*`)
- **Shizuku 13.1.5:** shell `settings/cmd phone/setprop` + binder `ICarrierConfigLoader.overrideConfig` / `ITelephony.setImsProvisioningInt` (persistent, Android 16 QPR2 bypass)
- **Giao diện:** High-density cũ (VoLtePrimary #005FB0) — 7 tabs, đơn giản cho người mới

## Build
```bash
export ANDROID_HOME=/tmp/android_sdk
./gradlew :app:assembleDebug
sha256sum app/build/outputs/apk/debug/app-debug.apk
apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk
```

## Credits
Xem `CREDITS.md` — **devprpvip / Hậu Minh** (minhhaulivetime@hotmail.com)

## License
**Apache License 2.0** — xem `LICENSE`
Copyright 2026 devprpvip / Hậu Minh
