# KernelSU Next 33278 → legacy 33024 compatibility build

Temporary build branch for a KernelSU Next Manager based on upstream commit `ac5de08b7702ba57aff64499a64f70e87d339252` (33278), while keeping compatibility with the pre-UAPI v3.1.0 / 33024 kernel line.

## Compatibility policy

- UAPI 4 matching kernel: normal/full mode.
- Pre-UAPI kernel (`kernelUAPIVersion == 0`) with KernelSU Next version >= 32310: legacy compatibility mode.
- Legacy mode keeps Superuser/Modules/root-required navigation available when root itself is working.
- Legacy mode does **not** run the current Manager's automatic `ksud install` on app startup, preserving the device's known-working `/data/adb/ksu` installation.
- The current Manager default SELinux domain `u:r:ksu:s0` is translated to the v3.1-era `u:r:su:s0` only for pre-UAPI kernels.
- Newer UAPI-only features are not emulated. Unsupported ioctls remain unsupported instead of being version-spoofed.

The workflow clones upstream source at the exact target commit, applies `patch_legacy_33024.py`, builds an arm64 Android `ksud`, then builds the Manager APK and uploads it as a GitHub Actions artifact.
