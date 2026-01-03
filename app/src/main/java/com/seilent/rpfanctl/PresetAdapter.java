package com.seilent.rpfanctl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;

public class PresetAdapter extends RecyclerView.Adapter<PresetAdapter.PresetViewHolder> {
    private ArrayList<Preset> presets;
    private String currentPresetUuid;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(Preset preset);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(int position);
    }

    public PresetAdapter(Context context, ArrayList<Preset> presets, String currentPresetUuid) {
        this.presets = presets;
        this.currentPresetUuid = currentPresetUuid;
    }

    public void setCurrentPreset(String uuid) {
        this.currentPresetUuid = uuid;
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
        holder.bind(preset, preset.getUuid().equals(currentPresetUuid));

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
            presetDetails.setText(createStyledCurveDetails(preset.getCurveDetails()));
            checkBox.setChecked(isSelected);

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

        private Spanned createStyledCurveDetails(String details) {
            SpannableString spannable = new SpannableString(details);

            int chipColor = ContextCompat.getColor(itemView.getContext(), R.color.md_theme_dark_surfaceVariant);
            int textColor = ContextCompat.getColor(itemView.getContext(), R.color.md_theme_dark_onSurfaceVariant);

            int chipStart = 0;
            boolean inChip = false;

            for (int i = 0; i < details.length(); i++) {
                char c = details.charAt(i);

                if (Character.isDigit(c) && !inChip) {
                    inChip = true;
                    chipStart = i;
                }

                if (c == ' ' && inChip) {
                    spannable.setSpan(new RoundedBackgroundSpan(chipColor, textColor), chipStart, i, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    inChip = false;
                }
            }

            if (inChip) {
                spannable.setSpan(new RoundedBackgroundSpan(chipColor, textColor), chipStart, details.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            return spannable;
        }

        private static class RoundedBackgroundSpan extends ReplacementSpan {
            private final int backgroundColor;
            private final int textColor;
            private static final float CORNER_RADIUS = 6f;
            private static final float PADDING_X = 12f;
            private static final float PADDING_Y = 3f;

            RoundedBackgroundSpan(int backgroundColor, int textColor) {
                this.backgroundColor = backgroundColor;
                this.textColor = textColor;
            }

            @Override
            public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
                return Math.round(paint.measureText(text, start, end) + PADDING_X * 2);
            }

            @Override
            public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
                float textWidth = paint.measureText(text, start, end);
                RectF rect = new RectF(x, top + PADDING_Y, x + textWidth + PADDING_X * 2, bottom - PADDING_Y);

                paint.setColor(backgroundColor);
                canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint);

                paint.setColor(textColor);
                canvas.drawText(text, start, end, x + PADDING_X, y, paint);
            }
        }
    }
}
