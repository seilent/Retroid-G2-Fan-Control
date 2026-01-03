#!/system/bin/sh

ui_print "================================"
ui_print "  RP Fan Control Daemon v1.0"
ui_print "================================"
ui_print "Installing fan control daemon..."

# Ensure the daemon exists in module package
DAEMON_SRC="$MODPATH/rpfanctld"
if [ ! -f "$DAEMON_SRC" ]; then
    ui_print "ERROR: Daemon not found at $DAEMON_SRC"
    ui_print "Installation failed!"
    abort "Daemon missing from module package"
fi

# Validate daemon script integrity before installation
if ! grep -q "daemon_main" "$DAEMON_SRC"; then
    ui_print "ERROR: Daemon script appears corrupted"
    abort "Daemon validation failed"
fi

# Create target directory
mkdir -p $MODPATH/system/bin

# Move daemon to system/bin
mv "$DAEMON_SRC" $MODPATH/system/bin/rpfanctld

# Set proper permissions (root:shell, 0755)
chmod 755 $MODPATH/system/bin/rpfanctld
chown 0:2000 $MODPATH/system/bin/rpfanctld

# Verify installation
if [ ! -x "$MODPATH/system/bin/rpfanctld" ]; then
    ui_print "ERROR: Failed to install daemon"
    abort "Daemon not executable after installation"
fi

ui_print "Daemon installed successfully"

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
