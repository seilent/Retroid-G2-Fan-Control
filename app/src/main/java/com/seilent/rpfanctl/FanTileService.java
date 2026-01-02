package com.seilent.rpfanctl;

import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.ArrayList;

public class FanTileService extends TileService {
    private static final String TAG = "FanTileService";
    private static final String PREFS = "FanPrefs";
    private static final String KEY_PRESETS = "presets";
    private static final String KEY_CURRENT_PRESET = "current_preset";

    @Override
    public void onClick() {
        super.onClick();
        boolean isCustomEnabled = RootHelper.isFanControlEnabled();

        if (isCustomEnabled) {
            showProfileSelector();
        } else {
            RootHelper.setFanControlEnabled(true);
            updateTile();
        }
    }

    private void showProfileSelector() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ArrayList<Preset> presets = loadPresets();
        String currentPresetName = RootHelper.getCurrentPreset();

        ArrayList<String> presetNames = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < presets.size(); i++) {
            String name = presets.get(i).getName();
            presetNames.add(name);
            if (name.equals(currentPresetName)) {
                selectedIndex = i;
            }
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Select Fan Profile")
                .setSingleChoiceItems(presetNames.toArray(new String[0]), selectedIndex, (d, which) -> {
                    Preset selected = presets.get(which);
                    prefs.edit().putString(KEY_CURRENT_PRESET, selected.toJson()).apply();
                    RootHelper.setFanCurve(selected.getPoints());
                    RootHelper.setCurrentPreset(selected.getName());
                    d.dismiss();
                    updateTile();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Disable", (d, which) -> {
                    RootHelper.setFanControlEnabled(false);
                    updateTile();
                })
                .create();

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private ArrayList<Preset> loadPresets() {
        ArrayList<Preset> list = new ArrayList<>();
        list.add(Preset.createDefault());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        java.util.Set<String> presetJsons = prefs.getStringSet(KEY_PRESETS, new java.util.HashSet<>());
        for (String json : presetJsons) {
            list.add(Preset.fromJson(json));
        }
        return list;
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile != null) {
            boolean isEnabled = RootHelper.isFanControlEnabled();

            tile.setState(isEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);

            if (isEnabled) {
                String profileName = RootHelper.getCurrentPreset();
                tile.setLabel("Fan: " + profileName);
                tile.setContentDescription("Fan control enabled - " + profileName);
            } else {
                tile.setLabel("Fan: OFF");
                tile.setContentDescription("Fan control disabled - tap to enable");
            }
            tile.updateTile();
        }
    }
}
