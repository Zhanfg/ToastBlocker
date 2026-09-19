#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parent / "KernelSU-Next"

def replace(path, old, new):
    p = ROOT / path
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"patch anchor not found: {path}\n--- anchor ---\n{old}")
    p.write_text(text.replace(old, new, 1))

# 1) Treat pre-UAPI KernelSU-Next 33024-class kernels as an explicit legacy backend.
replace(
    "manager/app/src/main/java/com/rifsxd/ksunext/Natives.kt",
    '    const val MINIMAL_SUPPORTED_KERNEL = 33188\n',
    '''    const val MINIMAL_SUPPORTED_KERNEL = 33188
    // Last pre-UAPI line known to work on the target device (v3.1.0 / 33024).
    // 32310 introduced the new allow-list ioctl used by the current manager.
    const val LEGACY_MINIMAL_SUPPORTED_KERNEL = 32310
'''
)

replace(
    "manager/app/src/main/java/com/rifsxd/ksunext/Natives.kt",
    '''    fun isFullFeatured(): Boolean {
        return isManager && kernelUAPIVersion == managerUAPIVersion && com.rifsxd.ksunext.ui.util.rootAvailable()
    }
''',
    '''    fun isLegacyCompatible(): Boolean {
        return isManager &&
            kernelUAPIVersion == 0 &&
            version >= LEGACY_MINIMAL_SUPPORTED_KERNEL
    }

    fun isFullFeatured(): Boolean {
        if (!isManager || !com.rifsxd.ksunext.ui.util.rootAvailable()) return false
        return kernelUAPIVersion == managerUAPIVersion || isLegacyCompatible()
    }
'''
)

# 2) Do not turn an otherwise-working legacy kernel into a global "kernel update required" state.
replace(
    "manager/app/src/main/java/com/rifsxd/ksunext/ui/screen/Home.kt",
    '''            val requiresNewKernel = isManager && kernelUAPIVersion != null && managerUAPIVersion > kernelUAPIVersion
''',
    '''            val requiresNewKernel = isManager &&
                kernelUAPIVersion != null &&
                !Natives.isLegacyCompatible() &&
                managerUAPIVersion > kernelUAPIVersion
'''
)

# 3) On legacy kernels, preserve the known-working /data/adb/ksu userspace installation.
# The manager still invokes its bundled ksud for UI operations, but does not overwrite
# persistent assets merely because the app was opened.
replace(
    "manager/app/src/main/java/com/rifsxd/ksunext/ui/MainActivity.kt",
    '''        val isManager = Natives.isManager
        if (isManager) install()
''',
    '''        val isManager = Natives.isManager
        if (isManager && !Natives.isLegacyCompatible()) install()
'''
)

replace(
    "manager/app/src/main/java/com/rifsxd/ksunext/ui/util/KsuCli.kt",
    '''fun install() {
    val start = SystemClock.elapsedRealtime()
''',
    '''fun install() {
    if (Natives.isLegacyCompatible()) {
        Log.i(TAG, "Legacy KernelSU-Next detected; preserving existing /data/adb/ksu assets")
        return
    }
    val start = SystemClock.elapsedRealtime()
'''
)

# 4) v3.1.0 used u:r:su:s0. The current manager defaults to u:r:ksu:s0.
# Remap only the current-manager default when talking to a pre-UAPI kernel.
replace(
    "manager/app/src/main/cpp/jni.cc",
    '''        auto cdomain = env->GetStringUTFChars((jstring) domain, nullptr);
        strcpy(p.rp_config.profile.selinux_domain, cdomain);
        env->ReleaseStringUTFChars((jstring) domain, cdomain);
''',
    '''        auto cdomain = env->GetStringUTFChars((jstring) domain, nullptr);
        if (get_kernel_uapi_version() == 0 && strcmp(cdomain, "u:r:ksu:s0") == 0) {
            strcpy(p.rp_config.profile.selinux_domain, "u:r:su:s0");
        } else {
            strcpy(p.rp_config.profile.selinux_domain, cdomain);
        }
        env->ReleaseStringUTFChars((jstring) domain, cdomain);
'''
)

print("KernelSU-Next legacy compatibility patches applied successfully.")
