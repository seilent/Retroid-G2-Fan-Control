#!/system/bin/sh
# Magisk module service script
# Started on boot, runs the fan control daemon

CONFIG_DIR="/data/adb/modules/rpfanctl"
DAEMON="/system/bin/rpfanctld"
PIDFILE="$CONFIG_DIR/daemon.pid"
LOGFILE="$CONFIG_DIR/daemon.log"

# Wait for system to be ready
sleep 30

# Create config directory
mkdir -p "$CONFIG_DIR"

# Write default config if not exists
if [ ! -f "$CONFIG_DIR/fan_config" ]; then
    cat > "$CONFIG_DIR/fan_config" << 'EOF'
# Fan control configuration
FAN_CURVE=20:0,50:10,70:15,80:20
ENABLED=0
EOF
fi

# Write default state
if [ ! -f "$CONFIG_DIR/fan_state" ]; then
    cat > "$CONFIG_DIR/fan_state" << 'EOF'
ENABLED=0
CURRENT_PRESET=Default
EOF
fi

# Check if already running
if [ -f "$PIDFILE" ]; then
    PID=$(cat "$PIDFILE")
    if [ -d "/proc/$PID" ]; then
        echo "Daemon already running (PID: $PID)" >> "$LOGFILE"
        exit 0
    fi
fi

# Start the daemon in background
nohup "$DAEMON" >/dev/null 2>&1 &
DAEMON_PID=$!

echo $DAEMON_PID > "$PIDFILE"
echo "Daemon started with PID: $DAEMON_PID" >> "$LOGFILE"

chmod 644 "$CONFIG_DIR"/*
chmod 755 "$DAEMON"
