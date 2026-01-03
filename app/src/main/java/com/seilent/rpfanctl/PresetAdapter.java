package com.seilent.rpfanctl;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;

public class PresetAdapter extends RecyclerView.Adapter<PresetAdapter.PresetViewHolder> {
    private ArrayList<Preset> presets;
    private String currentPresetName;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(Preset preset);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(int position);
    }

    public PresetAdapter(Context context, ArrayList<Preset> presets, String currentPresetName) {
        this.presets = presets;
        this.currentPresetName = currentPresetName;
    }

    public void setCurrentPreset(String name) {
        this.currentPresetName = name;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public PresetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_preset, parent, false);
        return new PresetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PresetViewHolder holder, int position) {
        Preset preset = presets.get(position);
        holder.bind(preset, preset.getName().equals(currentPresetName));

        // Set up click listeners
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(preset);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onItemLongClick(position);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return presets.size();
    }

    static class PresetViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private TextView presetName;
        private TextView presetDetails;
        private MaterialCheckBox checkBox;

        PresetViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            presetName = itemView.findViewById(R.id.preset_name);
            presetDetails = itemView.findViewById(R.id.preset_details);
            checkBox = itemView.findViewById(R.id.preset_checkbox);
        }

        void bind(Preset preset, boolean isSelected) {
            presetName.setText(preset.getName());
            presetDetails.setText(preset.toString());
            checkBox.setChecked(isSelected);

            // Update card appearance for selected state
            if (isSelected) {
                cardView.setStrokeWidth(2);
                cardView.setStrokeColor(cardView.getContext().getColor(
                        R.color.md_theme_light_primary));
                cardView.setCardElevation(4);
            } else {
                cardView.setStrokeWidth(0);
                cardView.setCardElevation(2);
            }
        }
    }
}
