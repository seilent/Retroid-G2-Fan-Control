package com.seilent.rpfanctl;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.TileService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private ListView presetList;
    private TextView currentStatus;
    private Switch customControlSwitch;
    private String currentPresetName;

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

        presetList = findViewById(R.id.preset_list);
        currentStatus = findViewById(R.id.current_status);
        customControlSwitch = findViewById(R.id.custom_control_switch);

        presetAdapter = new PresetAdapter(this, presets, currentPresetName);
        presetList.setAdapter(presetAdapter);

        presetList.setOnItemClickListener((parent, view, position, id) -> {
            Preset selected = presets.get(position);
            selectPreset(selected);
        });

        presetList.setOnItemLongClickListener((parent, view, position, id) -> {
            showEditDeleteDialog(position);
            return true;
        });

        boolean isCustomEnabled = RootHelper.isFanControlEnabled();
        customControlSwitch.setChecked(isCustomEnabled);
        customControlSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableCustomControl();
            } else {
                disableCustomControl();
            }
        });

        findViewById(R.id.btn_add_preset).setOnClickListener(v -> showAddPresetDialog(-1));

        statusUpdateHandler = new Handler(Looper.getMainLooper());
        statusUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateStatus();
                statusUpdateHandler.postDelayed(this, 1000);
            }
        };

        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        statusUpdateHandler.post(statusUpdateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        statusUpdateHandler.removeCallbacks(statusUpdateRunnable);
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
            .setTitle(isEdit ? "Edit Preset" : "Add Preset")
            .setView(dialogView)
            .setPositiveButton(isEdit ? "Save" : "Add", (d, which) -> {
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
            })
            .setNegativeButton("Cancel", null)
            .create();

        dialog.setOnShowListener(d -> {
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.9);
            int height = (int) (metrics.heightPixels * 0.85);
            dialog.getWindow().setLayout(width, height);
        });

        dialog.show();
    }

    private void updatePointEditButtons(LinearLayout container, FanCurveView graphView) {
        container.removeAllViews();

        List<Preset.TempPoint> points = graphView.getPoints();
        for (int i = 0; i < points.size(); i++) {
            Preset.TempPoint point = points.get(i);
            final int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, 4, 0, 4);

            TextView label = new TextView(this);
            label.setText("Point " + (index + 1) + ":");
            label.setTextSize(12);
            label.setPadding(0, 0, 16, 0);
            row.addView(label);

            Button tempBtn = new Button(this);
            tempBtn.setText(point.temperature + "°C");
            tempBtn.setTextSize(11);
            tempBtn.setPadding(16, 8, 16, 8);
            tempBtn.setBackgroundColor(isDarkMode() ? 0xFF333333 : 0xFFE0E0E0);
            tempBtn.setTextColor(0xFFFFFFFF);
            tempBtn.setOnClickListener(v -> showValueEditDialog("Temperature", index, true, graphView));
            row.addView(tempBtn, new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                1));

            TextView arrow = new TextView(this);
            arrow.setText("→");
            arrow.setTextSize(14);
            arrow.setPadding(16, 0, 16, 0);
            row.addView(arrow);

            Button fanBtn = new Button(this);
            fanBtn.setText(point.fanPercent + "%");
            fanBtn.setTextSize(11);
            fanBtn.setPadding(16, 8, 16, 8);
            fanBtn.setBackgroundColor(isDarkMode() ? 0xFF333333 : 0xFFE0E0E0);
            fanBtn.setTextColor(0xFFFFFFFF);
            fanBtn.setOnClickListener(v -> showValueEditDialog("Fan Speed", index, false, graphView));
            row.addView(fanBtn, new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                1));

            container.addView(row);
        }
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

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(40, 20, 40, 20);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView hint = new TextView(this);
        hint.setText(isTemp ? "Enter temperature (40-100°C):" : "Enter fan speed (0-100%):");
        hint.setTextSize(14);
        layout.addView(hint);

        EditText input = new EditText(this);
        input.setText(String.valueOf(isTemp ? point.temperature : point.fanPercent));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setSelectAllOnFocus(true);
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
                    point.fanPercent = value;
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

        String status = "Fan: " + percent + "% | Temp: " + temp + "°C";
        currentStatus.setText(status);
    }

    private class PresetAdapter extends ArrayAdapter<Preset> {
        private String currentPresetName;

        public PresetAdapter(Context context, ArrayList<Preset> presets, String currentPresetName) {
            super(context, android.R.layout.simple_list_item_1, presets);
            this.currentPresetName = currentPresetName;
        }

        public void setCurrentPreset(String name) {
            this.currentPresetName = name;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView textView = (TextView) view.findViewById(android.R.id.text1);

            Preset preset = getItem(position);
            if (preset != null && preset.getName().equals(currentPresetName)) {
                textView.setText("✓ " + preset.toString());
                textView.setTextColor(0xFF4FC3F7);
            } else {
                textView.setText(preset.toString());
                textView.setTextColor(0xFFFFFFFF);
            }

            return view;
        }
    }
}
