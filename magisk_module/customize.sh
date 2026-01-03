#!/system/bin/sh

ui_print "================================"
ui_print "  RP Fan Control Daemon v1.1"
ui_print "================================"
ui_print "Installing fan control daemon..."

set_perm_recursive $MODPATH/system/bin 0 2000 0755 0755

mkdir -p /data/adb/modules/rpfanctl

if [ ! -f "/data/adb/modules/rpfanctl/fan_config" ]; then
    cat > /data/adb/modules/rpfanctl/fan_config << 'EOF'
FAN_CURVE=20:0,50:10,70:15,80:20
ENABLED=0
EOF
fi

if [ ! -f "/data/adb/modules/rpfanctl/fan_state" ]; then
    cat > /data/adb/modules/rpfanctl/fan_state << 'EOF'
ENABLED=0
CURRENT_PRESET=Default
EOF
fi

ui_print "Installation complete!"
ui_print ""
ui_print "Install the companion app (RpFanCtl.apk) separately"
ui_print "to control fan settings."
