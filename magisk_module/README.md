# RP Fan Control Daemon

Custom fan control daemon for Retroid Pocket devices.

## Installation

1. Flash the Magisk module in Magisk Manager
2. Reboot your device
3. Install the companion APK (included as RpFanCtl.apk)
   - Via adb: `adb install -r /data/adb/modules/rpfanctl/RpFanCtl.apk`
   - Or use a file manager to install from the module directory

## Usage

1. Open the RpFanCtl app
2. Toggle "Enable custom fan control" to ON
3. Select a fan preset or create your own

The daemon runs in the background with root privileges and will:
- Automatically start on boot
- Monitor CPU temperature every 2 seconds
- Adjust fan speed according to the selected curve
- Re-enable manual mode if stock thermal control takes over

## Configuration Files

Located in `/data/adb/modules/rpfanctl/`:
- `fan_config` - Current fan curve configuration
- `fan_state` - Current state (enabled/disabled, selected preset)
- `daemon.log` - Daemon log file
- `daemon.pid` - Daemon process ID

## Default Fan Curve

- 20°C → 0%
- 50°C → 10%
- 70°C → 15%
- 80°C → 20%

## Uninstalling

1. Disable custom fan control in the app
2. Uninstall the Magisk module
3. Uninstall the RpFanCtl app
