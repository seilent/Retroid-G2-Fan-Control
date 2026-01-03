package com.seilent.rpfanctl;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.TileService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "FanPrefs";
    private static final String KEY_PRESETS = "presets";
    private static final String KEY_CURRENT_PRESET = "current_preset";

    private SharedPreferences prefs;
    private ArrayList<Preset> presets;
    private PresetAdapter presetAdapter;
    private RecyclerView presetRecyclerView;
    private TextView temperatureDisplay;
    private TextView fanSpeedDisplay;
    private MaterialSwitch customControlSwitch;
    private MaterialTextView controlLabel;
    private FloatingActionButton fabAddPreset;
    private String currentPresetName;
    private boolean isProgrammaticChange = false;

    private Handler statusUpdateHandler;
    private Runnable statusUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        presets = loadPresets();

        String currentPresetJson = prefs.getString(KEY_CURRENT_PRESET, null);
        if (currentPresetJson != null) {
            Preset currentPreset = Preset.fromJson(currentPresetJson);
            currentPresetName = currentPreset.getName();
        } else {
            currentPresetName = "Default";
        }

        // Initialize Material Components
        temperatureDisplay = findViewById(R.id.temperature_display);
        fanSpeedDisplay = findViewById(R.id.fan_speed_display);
        customControlSwitch = findViewById(R.id.custom_control_switch);
        controlLabel = findViewById(R.id.control_label);
        fabAddPreset = findViewById(R.id.fab_add_preset);
        presetRecyclerView = findViewById(R.id.preset_list);

        // Set up RecyclerView with LinearLayoutManager
        presetRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        presetAdapter = new PresetAdapter(this, presets, currentPresetName);
        presetRecyclerView.setAdapter(presetAdapter);

        // Set up click listeners
        presetAdapter.setOnItemClickListener(preset -> selectPreset(preset));
        presetAdapter.setOnItemLongClickListener(position -> {
            showEditDeleteDialog(position);
            return true;
        });

        boolean isCustomEnabled = RootHelper.isFanControlEnabled();
        customControlSwitch.setChecked(isCustomEnabled);
        updateControlLabel(isCustomEnabled);
        customControlSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;
            if (isChecked) {
                enableCustomControl();
            } else {
                disableCustomControl();
            }
        });

        // Replace button with FAB
        fabAddPreset.setOnClickListener(v -> showAddPresetDialog(-1));

        statusUpdateHandler = new Handler(Looper.getMainLooper());
        statusUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateStatus();
                // Also refresh switch state in case it was changed via tile
                isProgrammaticChange = true;
                boolean isCustomEnabled = RootHelper.isFanControlEnabled();
                customControlSwitch.setChecked(isCustomEnabled);
                updateControlLabel(isCustomEnabled);
                isProgrammaticChange = false;
                statusUpdateHandler.postDelayed(this, 1000);
            }
        };

        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        statusUpdateHandler.post(statusUpdateRunnable);

        // Refresh switch state in case it was changed via tile
        isProgrammaticChange = true;
        boolean isCustomEnabled = RootHelper.isFanControlEnabled();
        customControlSwitch.setChecked(isCustomEnabled);
        updateControlLabel(isCustomEnabled);
        isProgrammaticChange = false;
        updateStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        statusUpdateHandler.removeCallbacks(statusUpdateRunnable);
    }

    private void updateControlLabel(boolean isEnabled) {
        if (isEnabled) {
            controlLabel.setText("Enabled");
        } else {
            controlLabel.setText("Disabled");
        }
    }

    private void enableCustomControl() {
        String presetJson = prefs.getString(KEY_CURRENT_PRESET, null);
        Preset preset;
        if (presetJson != null) {
            preset = Preset.fromJson(presetJson);
        } else {
            preset = Preset.createDefault();
            prefs.edit().putString(KEY_CURRENT_PRESET, preset.toJson()).apply();
        }

        RootHelper.setFanCurve(preset.getPoints());
        RootHelper.setFanControlEnabled(true);
        RootHelper.setCurrentPreset(preset.getName());

        requestTileUpdate();
        Toast.makeText(this, "Custom fan control enabled", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void disableCustomControl() {
        RootHelper.setFanControlEnabled(false);

        requestTileUpdate();
        Toast.makeText(this, "Reverted to stock fan control", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void requestTileUpdate() {
        TileService.requestListeningState(this, new ComponentName(this, FanTileService.class));
    }

    private ArrayList<Preset> loadPresets() {
        ArrayList<Preset> list = new ArrayList<>();
        list.add(Preset.createDefault());

        Set<String> presetJsons = prefs.getStringSet(KEY_PRESETS, new HashSet<>());
        for (String json : presetJsons) {
            list.add(Preset.fromJson(json));
        }
        return list;
    }

    private void savePresets() {
        Set<String> presetJsons = new HashSet<>();
        for (int i = 1; i < presets.size(); i++) {
            presetJsons.add(presets.get(i).toJson());
        }
        prefs.edit().putStringSet(KEY_PRESETS, presetJsons).apply();
    }

    private ArrayList<String> getPresetNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Preset p : presets) {
            names.add(p.toString());
        }
        return names;
    }

    private void selectPreset(Preset preset) {
        currentPresetName = preset.getName();
        presetAdapter.setCurrentPreset(currentPresetName);
        prefs.edit().putString(KEY_CURRENT_PRESET, preset.toJson()).apply();

        RootHelper.setFanCurve(preset.getPoints());
        RootHelper.setCurrentPreset(preset.getName());

        Toast.makeText(this, "Selected: " + preset.getName(), Toast.LENGTH_SHORT).show();
        presetAdapter.notifyDataSetChanged();
        updateStatus();
    }

    private void showEditDeleteDialog(int position) {
        if (position == 0) {
            new AlertDialog.Builder(this)
                .setTitle(presets.get(position).getName())
                .setMessage("This is the default preset and cannot be edited.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        Preset preset = presets.get(position);
        String[] options = {"Edit", "Delete"};

        new AlertDialog.Builder(this)
            .setTitle(preset.getName())
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    showAddPresetDialog(position);
                } else {
                    showDeleteDialog(position);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showDeleteDialog(int position) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Preset")
            .setMessage("Delete " + presets.get(position).getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                presets.remove(position);
                savePresets();
                refreshPresetList();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAddPresetDialog(int editPosition) {
        final boolean isEdit = editPosition >= 0;
        final Preset editingPreset = isEdit ? presets.get(editPosition) : null;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_preset_graph, null);
        EditText nameInput = dialogView.findViewById(R.id.preset_name);
        FanCurveView graphView = dialogView.findViewById(R.id.fan_curve_graph);
        LinearLayout pointEditContainer = dialogView.findViewById(R.id.point_edit_container);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnSave.setText(isEdit ? "Save" : "Add");

        if (isEdit) {
            nameInput.setText(editingPreset.getName());
            graphView.setPoints(editingPreset.getPoints());
        } else {
            List<Preset.TempPoint> defaultPoints = new ArrayList<>();
            defaultPoints.add(new Preset.TempPoint(20, 0));
            defaultPoints.add(new Preset.TempPoint(50, 10));
            defaultPoints.add(new Preset.TempPoint(70, 15));
            defaultPoints.add(new Preset.TempPoint(80, 20));
            graphView.setPoints(defaultPoints);
        }

        updatePointEditButtons(pointEditContainer, graphView);

        graphView.setOnPointChangedListener((index, temp, fan) -> {
            updatePointEditButtons(pointEditContainer, graphView);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();

        btnSave.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                name = "Custom Preset";
            }

            List<Preset.TempPoint> points = graphView.getPoints();
            if (points.isEmpty()) {
                Toast.makeText(this, "Please add at least one point", Toast.LENGTH_SHORT).show();
                return;
            }

            Preset newPreset = new Preset(name, points);

            if (isEdit) {
                presets.set(editPosition, newPreset);
            } else {
                presets.add(newPreset);
            }
            savePresets();
            refreshPresetList();
            Toast.makeText(this, isEdit ? "Preset saved" : "Preset added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnShowListener(d -> {
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            dialog.getWindow().setLayout(metrics.widthPixels, metrics.heightPixels);
        });

        dialog.show();
    }

    private void updatePointEditButtons(LinearLayout container, FanCurveView graphView) {
        container.removeAllViews();

        // Get consistent spacing and colors from resources
        int spacingXs = getResources().getDimensionPixelSize(R.dimen.spacing_xs);
        int spacingSm = getResources().getDimensionPixelSize(R.dimen.spacing_sm);
        int pointRowPaddingBottom = spacingSm;
        int pointRowPaddingTop = spacingXs;

        // Get colors based on theme
        int buttonBgColor = getColor(isDarkMode() ? R.color.md_theme_dark_surfaceVariant : R.color.md_theme_light_surfaceVariant);
        int buttonTextColor = getColor(isDarkMode() ? R.color.md_theme_dark_onSurfaceVariant : R.color.md_theme_light_onSurfaceVariant);
        int labelTextColor = getColor(android.R.color.darker_gray);

        List<Preset.TempPoint> points = graphView.getPoints();
        for (int i = 0; i < points.size(); i++) {
            Preset.TempPoint point = points.get(i);
            final int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, pointRowPaddingTop, 0, pointRowPaddingBottom);

            // Point label
            TextView label = new TextView(this);
            label.setText("Point " + (index + 1));
            label.setTextAppearance(android.R.style.TextAppearance_Small);
            label.setTextColor(labelTextColor);
            row.addView(label);

            // Values row
            LinearLayout valuesRow = new LinearLayout(this);
            valuesRow.setOrientation(LinearLayout.HORIZONTAL);
            valuesRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Temperature button
            Button tempBtn = createValueButton(point.temperature + "°", buttonBgColor, buttonTextColor);
            tempBtn.setOnClickListener(v -> showValueEditDialog("Temperature", index, true, graphView));
            valuesRow.addView(tempBtn);

            // Arrow
            TextView arrow = new TextView(this);
            arrow.setText("→");
            arrow.setTextAppearance(android.R.style.TextAppearance_Medium);
            arrow.setPadding(spacingSm, 0, spacingSm, 0);
            valuesRow.addView(arrow);

            // Fan speed button
            Button fanBtn = createValueButton(point.fanPercent + "%", buttonBgColor, buttonTextColor);
            fanBtn.setOnClickListener(v -> showValueEditDialog("Fan Speed", index, false, graphView));
            valuesRow.addView(fanBtn);

            row.addView(valuesRow);
            container.addView(row);
        }
    }

    private Button createValueButton(String text, int bgColor, int textColor) {
        Button btn = new Button(this, null, android.R.attr.buttonBarButtonStyle);
        btn.setText(text);
        btn.setBackgroundColor(bgColor);
        btn.setTextColor(textColor);
        btn.setPadding(
            getResources().getDimensionPixelSize(R.dimen.spacing_sm),
            getResources().getDimensionPixelSize(R.dimen.spacing_xs),
            getResources().getDimensionPixelSize(R.dimen.spacing_sm),
            getResources().getDimensionPixelSize(R.dimen.spacing_xs)
        );
        btn.setMinimumHeight(
            getResources().getDimensionPixelSize(R.dimen.touch_target_min)
        );
        return btn;
    }

    private boolean isDarkMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return getResources().getConfiguration().isNightModeActive();
        }
        int nightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void showValueEditDialog(String title, int pointIndex, boolean isTemp, FanCurveView graphView) {
        List<Preset.TempPoint> points = graphView.getPoints();
        Preset.TempPoint point = points.get(pointIndex);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title + " - Point " + (pointIndex + 1));

        // Use consistent padding from dimens
        int paddingXl = getResources().getDimensionPixelSize(R.dimen.spacing_xl);
        int paddingLg = getResources().getDimensionPixelSize(R.dimen.spacing_lg);

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(paddingXl, paddingLg, paddingXl, paddingLg);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView hint = new TextView(this);
        hint.setText(isTemp ? "Enter temperature (0-100°C):" : "Enter fan speed (0-100%):");
        hint.setTextAppearance(android.R.style.TextAppearance_Medium);
        layout.addView(hint);

        EditText input = new EditText(this);
        input.setText(String.valueOf(isTemp ? point.temperature : point.fanPercent));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setSelectAllOnFocus(true);
        // Set consistent width for input
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            getResources().getDimensionPixelSize(R.dimen.spacing_xl) * 5,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        input.setLayoutParams(params);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Set", (dialog, which) -> {
            try {
                int value = Integer.parseInt(input.getText().toString());

                if (isTemp) {
                    if (value < 0 || value > 100) {
                        Toast.makeText(this, "Temperature must be 0-100°C", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int minTemp = (pointIndex > 0) ? points.get(pointIndex - 1).temperature + 1 : 0;
                    int maxTemp = (pointIndex < points.size() - 1) ? points.get(pointIndex + 1).temperature - 1 : 100;
                    if (value < minTemp || value > maxTemp) {
                        Toast.makeText(this, "Temperature must be between " + minTemp + " and " + maxTemp + "°C", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    point.temperature = value;
                } else {
                    if (value < 0 || value > 100) {
                        Toast.makeText(this, "Fan speed must be 0-100%", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Enforce monotonic: can't go below previous point
                    int minFan = (pointIndex > 0) ? points.get(pointIndex - 1).fanPercent : 0;
                    if (value < minFan) {
                        Toast.makeText(this, "Fan speed must be at least " + minFan + "%", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Update point and push subsequent points up if needed
                    point.fanPercent = value;
                    for (int i = pointIndex + 1; i < points.size(); i++) {
                        if (points.get(i).fanPercent < value) {
                            points.get(i).fanPercent = value;
                        } else {
                            break;
                        }
                    }
                }

                graphView.invalidate();
                android.view.ViewParent parent = graphView.getParent();
                if (parent instanceof android.view.View) {
                    android.view.View parentView = (android.view.View) parent;
                    android.view.View container = parentView.findViewById(R.id.point_edit_container);
                    if (container instanceof LinearLayout) {
                        updatePointEditButtons((LinearLayout) container, graphView);
                    }
                }

                if (graphView.onPointChangedListener != null) {
                    graphView.onPointChangedListener.onPointChanged(pointIndex, point.temperature, point.fanPercent);
                }

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid value", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void refreshPresetList() {
        presetAdapter.notifyDataSetChanged();
    }

    private void updateStatus() {
        int duty = RootHelper.getFanDuty();
        int percent = Preset.dutyToPercent(duty);
        int temp = RootHelper.getCpuTemp() / 1000;

        // Update separate displays
        temperatureDisplay.setText(temp + "°C");
        fanSpeedDisplay.setText(percent + "%");

        // Update temperature text color based on threshold
        int tempColor;
        if (temp >= 80) {
            tempColor = getColor(R.color.md_theme_light_error);
        } else if (temp >= 60) {
            tempColor = getColor(R.color.md_theme_light_primary);
        } else {
            tempColor = getColor(R.color.md_theme_light_secondary);
        }
        temperatureDisplay.setTextColor(tempColor);
    }
}
