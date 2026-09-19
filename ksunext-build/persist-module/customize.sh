#!/system/bin/sh
ui_print "- KernelSU Next legacy Manager persistence"
ui_print "- Target: com.rifsxd.ksunext.compat33024"
ui_print "- Safe-mode/disable/remove restores the original Manager path"
set_perm "$MODPATH/bin/claim_manager" 0 0 0755
set_perm "$MODPATH/common.sh" 0 0 0755
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
