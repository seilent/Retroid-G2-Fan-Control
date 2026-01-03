package com.seilent.rpfanctl;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class FanTileService extends TileService {
    private static final String TAG = "FanTileService";

    @Override
    public void onClick() {
        super.onClick();
        boolean isCustomEnabled = RootHelper.isFanControlEnabled();

        if (isCustomEnabled) {
            RootHelper.setFanControlEnabled(false);
        } else {
            RootHelper.setFanControlEnabled(true);
        }
        updateTile();
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
