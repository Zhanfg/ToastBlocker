#!/system/bin/sh
PKG_COMPAT="com.rifsxd.ksunext.compat33024"
PKG_ORIGINAL="com.rifsxd.ksunext"
MODDIR="${0%/*}"
CLAIM_BIN="$MODDIR/bin/claim_manager"
LOGFILE="$MODDIR/claim.log"

find_compat_appid() {
  [ -r /data/system/packages.list ] || return 1
  uid="$(awk -v p="$PKG_COMPAT" '$1 == p { print $2; exit }' /data/system/packages.list 2>/dev/null)"
  [ -n "$uid" ] || return 1
  appid=$((uid % 100000))
  [ "$appid" -gt 10000 ] && [ "$appid" -lt 20000 ] || return 1
  echo "$appid"
}

claim_compat_manager() {
  appid="$(find_compat_appid)" || return 1
  {
    echo "[$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null)] claim appid=$appid"
    "$CLAIM_BIN" "$appid"
    rc=$?
    echo "result=$rc"
  } >> "$LOGFILE" 2>&1
  return $rc
}
