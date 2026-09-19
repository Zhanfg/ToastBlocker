#!/system/bin/sh
MODDIR="${0%/*}"
. "$MODDIR/common.sh"

# Wait until the Compat package is present in packages.list, then claim it.
i=0
while [ "$i" -lt 90 ]; do
  if claim_compat_manager; then
    break
  fi
  i=$((i + 1))
  sleep 1
done

# Reassert after Android reports boot completed. v3.1.0 runs throne maintenance
# around boot/package events, so this closes the startup race deterministically.
i=0
while [ "$i" -lt 120 ] && [ "$(getprop sys.boot_completed 2>/dev/null)" != "1" ]; do
  i=$((i + 1))
  sleep 1
done

claim_compat_manager || true
sleep 3
claim_compat_manager || true

# Any process created before the AppID switch may hold an old manager FD.
# Closing both Manager processes makes the next launch inherit the correct state.
am force-stop "$PKG_ORIGINAL" >/dev/null 2>&1 || true
am force-stop "$PKG_COMPAT" >/dev/null 2>&1 || true
