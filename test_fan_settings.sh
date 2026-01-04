#!/system/bin/sh

# Fan Settings Test Script
# Tests the new settings-based approach vs direct sysfs writes

DUTY_SYSFS="/sys/class/gpio5_pwm2/duty"
THERMAL_SYSFS="/sys/class/gpio5_pwm2/thermal"
TEMP_SYSFS="/sys/class/thermal/thermal_zone0/temp"

log() {
    echo "[$(date +%H:%M:%S)] $1"
}

get_current_state() {
    echo "=== Current State ==="
    echo "fan_mode: $(settings get system fan_mode)"
    echo "fan_speed: $(settings get system fan_speed)"
    echo "performance_mode: $(settings get system performance_mode)"
    echo "duty sysfs: $(cat $DUTY_SYSFS 2>/dev/null || echo 'error')"
    echo "thermal sysfs: $(cat $THERMAL_SYSFS 2>/dev/null || echo 'error')"
    echo "temperature: $(( $(cat $TEMP_SYSFS 2>/dev/null || echo 0) / 1000 ))°C"
    echo ""
}

# Test 1: Basic settings write and verify
test_basic_settings() {
    log "Test 1: Basic Settings Commands"
    settings put system fan_mode 6
    settings put system fan_speed 25000
    sleep 1
    get_current_state
    # Verify duty matches fan_speed in mode 6
    DUTY=$(cat $DUTY_SYSFS)
    if [ "$DUTY" = "25000" ]; then
        log "PASS: Duty matches fan_speed setting"
    else
        log "FAIL: Duty=$DUTY, expected 25000"
    fi
}

# Test 2: Range test (0, 25000, 50000)
test_range() {
    log "Test 2: Range Test"
    for speed in 0 25000 50000; do
        settings put system fan_speed $speed
        sleep 0.5
        DUTY=$(cat $DUTY_SYSFS)
        log "fan_speed=$speed -> duty=$DUTY"
    done
}

# Test 3: Mode multipliers (4=Smart, 5=Sport, 6=Customize)
test_multipliers() {
    log "Test 3: Mode Multipliers"
    settings put system fan_speed 20000
    for mode in 4 5 6; do
        settings put system fan_mode $mode
        sleep 1
        DUTY=$(cat $DUTY_SYSFS)
        log "Mode $mode: fan_speed=20000 -> duty=$DUTY"
    done
}

# Test 4: Conflict test (monitor for 60 seconds)
test_conflicts() {
    log "Test 4: Conflict Monitoring (60 seconds)"
    settings put system fan_mode 6
    settings put system fan_speed 30000
    log "Monitoring for thermal HAL interference..."
    for i in $(seq 1 60); do
        DUTY=$(cat $DUTY_SYSFS)
        THERMAL=$(cat $THERMAL_SYSFS)
        log "[$i/60] duty=$DUTY thermal=$THERMAL"
        sleep 1
    done
}

# Test 5: Performance mode interaction
test_performance_mode() {
    log "Test 5: Performance Mode Interaction"
    settings put system fan_mode 6
    settings put system fan_speed 25000
    log "Before perf mode change:"
    get_current_state
    settings put system performance_mode 0  # Standard
    sleep 2
    log "After perf_mode=0 (Standard):"
    get_current_state
}

# Run all tests
log "=== Fan Settings Test Suite ==="
log ""
get_current_state
test_basic_settings
test_range
test_multipliers
test_performance_mode
# test_conflicts  # Uncomment for long-running test
log "=== Tests Complete ==="
