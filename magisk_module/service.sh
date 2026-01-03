#!/system/bin/sh

CONFIG_DIR="/data/adb/modules/rpfanctl"
DAEMON="/system/bin/rpfanctld"
PIDFILE="$CONFIG_DIR/daemon.pid"
LOGFILE="$CONFIG_DIR/daemon.log"

sleep 30

mkdir -p "$CONFIG_DIR"

if [ ! -f "$CONFIG_DIR/fan_config" ]; then
    cat > "$CONFIG_DIR/fan_config" << 'EOF'
FAN_CURVE=20:0,50:10,70:15,80:20
ENABLED=0
EOF
fi

if [ ! -f "$CONFIG_DIR/fan_state" ]; then
    cat > "$CONFIG_DIR/fan_state" << 'EOF'
ENABLED=0
CURRENT_PRESET=Default
EOF
fi

if [ -f "$PIDFILE" ]; then
    PID=$(cat "$PIDFILE")
    if [ -d "/proc/$PID" ]; then
        echo "Daemon already running (PID: $PID)" >> "$LOGFILE"
        exit 0
    fi
fi

nohup "$DAEMON" >/dev/null 2>&1 &
DAEMON_PID=$!

echo $DAEMON_PID > "$PIDFILE"
echo "Daemon started with PID: $DAEMON_PID" >> "$LOGFILE"

chmod 644 "$CONFIG_DIR"/*
chmod 755 "$DAEMON"
