package com.seilent.rpfanctl;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RootHelper {
    private static final String TAG = "RootHelper";
    private static final String MODULE_DIR = "/data/adb/modules/rpfanctl";
    private static final String CONFIG_FILE = MODULE_DIR + "/fan_config";
    private static final String STATE_FILE = MODULE_DIR + "/fan_state";

    public static boolean isRootAvailable() {
        return executeShell("echo test") != null;
    }

    public static String executeShell(String command) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            p.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "Error executing command: " + command, e);
            return null;
        }
    }

    public static boolean writeToFile(String path, String value) {
        String result = executeShell("echo " + value + " > " + path);
        return result != null;
    }

    public static String readFile(String path) {
        return executeShell("cat " + path);
    }

    public static int getFanDuty() {
        String value = readFile("/sys/class/gpio5_pwm2/duty");
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing duty value", e);
            }
        }
        return 0;
    }

    public static void setFanDuty(int duty) {
        executeShell("settings put system fan_mode 6");
        executeShell("settings put system fan_speed " + duty);
    }

    public static int getCpuTemp() {
        String temp = readFile("/sys/class/thermal/thermal_zone0/temp");
        if (temp != null) {
            try {
                return Integer.parseInt(temp.trim());
            } catch (NumberFormatException e) {
            }
        }
        return 0;
    }

    public static void resetToStock() {
        executeShell("settings put system performance_mode 1");
        executeShell("settings put system fan_mode 4");
    }

    public static void setFanControlEnabled(boolean enabled) {
        String value = enabled ? "1" : "0";
        executeShell("sed -i 's/^ENABLED=.*/ENABLED=" + value + "/' " + STATE_FILE);
        if (!enabled) {
            executeShell("settings put system performance_mode 1");
            executeShell("settings put system fan_mode 4");
        } else {
            executeShell("settings put system fan_mode 6");
        }
    }

    public static boolean isFanControlEnabled() {
        String state = readFile(STATE_FILE);
        if (state != null) {
            String[] lines = state.split("\n");
            for (String line : lines) {
                if (line.startsWith("ENABLED=")) {
                    return line.substring(8).trim().equals("1");
                }
            }
        }
        return false;
    }

    public static void setFanCurve(java.util.List<Preset.TempPoint> points) {
        StringBuilder curve = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) curve.append(",");
            curve.append(points.get(i).temperature).append(":").append(points.get(i).fanPercent);
        }
        executeShell("sed -i 's|^FAN_CURVE=.*|FAN_CURVE=" + curve.toString() + "|' " + CONFIG_FILE);
    }

    public static String getCurrentPreset() {
        String state = readFile(STATE_FILE);
        if (state != null) {
            String[] lines = state.split("\n");
            for (String line : lines) {
                if (line.startsWith("CURRENT_PRESET=")) {
                    return line.substring(15).trim();
                }
            }
        }
        return "Default";
    }

    public static void setCurrentPreset(String name) {
        executeShell("sed -i 's|^CURRENT_PRESET=.*|CURRENT_PRESET=" + name + "|' " + STATE_FILE);
    }

    public static void setCurrentPreset(String name, String uuid) {
        executeShell("sed -i 's|^CURRENT_PRESET=.*|CURRENT_PRESET=" + name + "|' " + STATE_FILE);
        executeShell("sed -i 's|^CURRENT_PRESET_UUID=.*|CURRENT_PRESET_UUID=" + uuid + "|' " + STATE_FILE);
    }

    public static String getCurrentPresetUuid() {
        String state = readFile(STATE_FILE);
        if (state != null) {
            String[] lines = state.split("\n");
            for (String line : lines) {
                if (line.startsWith("CURRENT_PRESET_UUID=")) {
                    return line.substring(19).trim();
                }
            }
        }
        return null;
    }
}
