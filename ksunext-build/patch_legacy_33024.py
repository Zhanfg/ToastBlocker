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

# 4) v3.1.0 uses App Profile ABI v2 and SELinux domain u:r:su:s0.
# The current ABI is v4. The layout stayed prefix-compatible, but explicitly
# sending version 2 avoids relying on the old kernel accepting a future version.
replace(
    "manager/app/src/main/cpp/jni.cc",
    '''    app_profile profile = {};
    profile.version = KSU_APP_PROFILE_VER;
''',
    '''    app_profile profile = {};
    profile.version = get_kernel_uapi_version() == 0 ? 2 : KSU_APP_PROFILE_VER;
'''
)

replace(
    "manager/app/src/main/cpp/jni.cc",
    '''    app_profile p = {};
    p.version = KSU_APP_PROFILE_VER;
''',
    '''    app_profile p = {};
    p.version = get_kernel_uapi_version() == 0 ? 2 : KSU_APP_PROFILE_VER;
'''
)

# v3.1.0 used u:r:su:s0. The current manager defaults to u:r:ksu:s0.
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

# 5) Add a root-only manager takeover command to ksud. KernelSU Next v3.1.0
# already implements CHANGE_MANAGER_UID=10006 in the reboot syscall hook.
replace(
    "userspace/ksud/src/ksucalls.rs",
    '''const DRIVER_FD_NAME: &str = "anon_inode:[ksu_driver]";
''',
    '''pub fn change_manager_uid(appid: u32) -> Result<()> {
    if !(10001..20000).contains(&appid) {
        bail!("legacy toolkit accepts Android appId 10001..19999, got {appid}");
    }

    let mut marker: usize = 0;
    let marker_ptr = &mut marker as *mut usize;
    let expected = marker_ptr as usize;

    unsafe {
        libc::syscall(
            libc::SYS_reboot,
            ksu_uapi::KSU_INSTALL_MAGIC1 as libc::c_ulong,
            ksu_uapi::CHANGE_MANAGER_UID as libc::c_ulong,
            appid as libc::c_ulong,
            marker_ptr,
        );
    }

    if marker != expected {
        bail!("KernelSU CHANGE_MANAGER_UID was not acknowledged");
    }
    Ok(())
}

const DRIVER_FD_NAME: &str = "anon_inode:[ksu_driver]";
'''
)

replace(
    "userspace/ksud/src/cli.rs",
    '''    SetManager {
        /// manager package name
        #[arg(default_value_t = String::from("com.rifsxd.ksunext"))]
        apk: String,
    },

    /// Get apk size and hash
''',
    '''    SetManager {
        /// manager package name
        #[arg(default_value_t = String::from("com.rifsxd.ksunext"))]
        apk: String,
    },

    /// Claim KernelSU manager AppID through the root-only legacy toolkit interface.
    ClaimManager {
        /// Android appId (uid modulo 100000)
        appid: u32,
    },

    /// Get apk size and hash
'''
)

replace(
    "userspace/ksud/src/cli.rs",
    '''        Commands::Debug { command } => match command {
            Debug::SetManager { apk } => debug::set_manager(&apk),
            Debug::GetSign { apk } => {
''',
    '''        Commands::Debug { command } => match command {
            Debug::SetManager { apk } => debug::set_manager(&apk),
            Debug::ClaimManager { appid } => ksucalls::change_manager_uid(appid),
            Debug::GetSign { apk } => {
'''
)

# 6) Expose manager takeover to the Android app. It is executed inside an already
# granted root shell, so the old signed Manager remains the trust bootstrap.
replace(
    "manager/app/src/main/java/com/rifsxd/ksunext/ui/util/KsuCli.kt",
    '''fun execKsud(args: String, newShell: Boolean = false, globalMnt: Boolean = false): Boolean {
    return if (newShell) {
        withNewRootShell(globalMnt = globalMnt) {
            ShellUtils.fastCmdResult(this, "${getKsuDaemonPath()} $args")
        }
    } else {
        ShellUtils.fastCmdResult("${getKsuDaemonPath()} $args")
    }
}
''',
    '''fun execKsud(args: String, newShell: Boolean = false, globalMnt: Boolean = false): Boolean {
    return if (newShell) {
        withNewRootShell(globalMnt = globalMnt) {
            ShellUtils.fastCmdResult(this, "${getKsuDaemonPath()} $args")
        }
    } else {
        ShellUtils.fastCmdResult("${getKsuDaemonPath()} $args")
    }
}

fun claimManagerAppId(appId: Int): Boolean {
    if (appId !in 10000..99999) return false
    return execKsud("debug claim-manager $appId", true)
}
'''
)

# 7) Bootstrap a differently-signed compatibility Manager:
#    old signed Manager grants root once -> Compat calls CHANGE_MANAGER_UID -> restart.
replace(
    "manager/app/src/main/java/com/rifsxd/ksunext/ui/MainActivity.kt",
    '''import kotlinx.coroutines.launch
''',
    '''import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
'''
)

replace(
    "manager/app/src/main/java/com/rifsxd/ksunext/ui/MainActivity.kt",
    '''class MainActivity : ComponentActivity() {

    var zipUri by mutableStateOf<ArrayList<Uri>?>(null)
''',
    '''class MainActivity : ComponentActivity() {

    private var legacyClaimRunning = false
    private var legacyHintShown = false

    override fun onResume() {
        super.onResume()

        if (Natives.isManager || legacyClaimRunning) return
        legacyClaimRunning = true

        lifecycleScope.launch(Dispatchers.IO) {
            // Force one real su request. Before takeover, the bundled ksud cannot grant
            // root itself, so the shell builder falls back to the legacy KernelSU su.
            val hasRoot = runCatching {
                createRootShell(false).use { shell ->
                    shell.newJob()
                        .add("id -u")
                        .exec()
                        .out
                        .firstOrNull()
                        ?.trim() == "0"
                }
            }.getOrDefault(false)

            if (!hasRoot) {
                withContext(Dispatchers.Main) {
                    legacyClaimRunning = false
                    if (!legacyHintShown) {
                        legacyHintShown = true
                        Toast.makeText(
                            this@MainActivity,
                            "请在旧版 KernelSU Next 的授权弹窗中允许 KernelSU Next Compat 获取 root，然后返回本应用",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                return@launch
            }

            val appId = android.os.Process.myUid() % 100000
            val claimed = claimManagerAppId(appId)

            withContext(Dispatchers.Main) {
                if (!claimed) {
                    legacyClaimRunning = false
                    Toast.makeText(
                        this@MainActivity,
                        "Manager 接管失败：当前内核未确认 CHANGE_MANAGER_UID",
                        Toast.LENGTH_LONG
                    ).show()
                    return@withContext
                }

                Toast.makeText(
                    this@MainActivity,
                    "Manager 接管成功，正在重新启动 Compat Manager",
                    Toast.LENGTH_SHORT
                ).show()

                handler.postDelayed({
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    launchIntent?.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    }
                    android.os.Process.killProcess(android.os.Process.myPid())
                }, 600)
            }
        }
    }

    var zipUri by mutableStateOf<ArrayList<Uri>?>(null)
'''
)

# 8) Make the compatibility build distinguishable from the official app.
replace(
    "manager/app/src/main/res/values/strings.xml",
    '''    <string name="app_name" translatable="false">KernelSU-Next</string>
''',
    '''    <string name="app_name" translatable="false">KernelSU Next Compat</string>
'''
)

print("KernelSU-Next legacy compatibility patches applied successfully.")
