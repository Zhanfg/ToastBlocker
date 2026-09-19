#!/system/bin/sh
MODDIR="${0%/*}"
. "$MODDIR/common.sh"

# Best-effort early claim. service.sh repeats this after PackageManager is ready.
claim_compat_manager || true
