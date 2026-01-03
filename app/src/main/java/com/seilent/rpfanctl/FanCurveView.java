package com.seilent.rpfanctl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.ColorRes;
import android.graphics.Path;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FanCurveView extends View {
    private List<Preset.TempPoint> points;

    private static final int MIN_TEMP = 0;
    private static final int MAX_TEMP = 100;
    private static final int MIN_FAN = 0;
    private static final int MAX_FAN = 100;

    private RectF graphArea;
    private int paddingLeft, paddingTop, paddingRight, paddingBottom;

    private int selectedPointIndex = -1;
    private boolean isDragging = false;
    private boolean hasMovedBeyondSlop;
    private float touchSlop;
    private float touchStartX, touchStartY;
    private static final int TOUCH_TOLERANCE_DP = 60;

    private Paint gridPaint;
    private Paint axisPaint;
    private Paint curvePaint;
    private Paint pointPaint;
    private Paint selectedPointPaint;
    private Paint textPaint;
    private Paint fillPaint;
    private Paint backgroundPaint;

    private int pointRadius;
    private int selectedPointRadius;

    private boolean isDarkMode;

    public FanCurveView(Context context) {
        super(context);
        init();
    }

    public FanCurveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FanCurveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        points = new ArrayList<>();

        isDarkMode = isDarkMode();

        points.add(new Preset.TempPoint(20, 0));
        points.add(new Preset.TempPoint(50, 10));
        points.add(new Preset.TempPoint(70, 15));
        points.add(new Preset.TempPoint(80, 20));

        float density = getResources().getDisplayMetrics().density;
        paddingLeft = (int) (40 * density);
        paddingRight = (int) (12 * density);
        paddingTop = (int) (24 * density);
        paddingBottom = (int) (28 * density);

        pointRadius = (int) (16 * density);
        selectedPointRadius = (int) (20 * density);

        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        setClickable(true);
        setFocusable(true);

        initPaints(density);
    }

    private boolean isDarkMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getResources().getConfiguration().isNightModeActive();
        }
        int nightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private int getColor(@ColorRes int colorId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getResources().getColor(colorId, getContext().getTheme());
        } else {
            return getResources().getColor(colorId);
        }
    }

    private void initPaints(float density) {
        int bgColor = getColor(isDarkMode ? R.color.fan_curve_dark_background : R.color.fan_curve_light_background);
        int gridColor = getColor(isDarkMode ? R.color.fan_curve_dark_grid : R.color.fan_curve_light_grid);
        int axisColor = getColor(isDarkMode ? R.color.fan_curve_dark_axis : R.color.fan_curve_light_axis);
        int textColor = getColor(isDarkMode ? R.color.fan_curve_dark_text : R.color.fan_curve_light_text);
        int pointColor = getColor(isDarkMode ? R.color.fan_curve_dark_point : R.color.fan_curve_light_point);
        int curveColor = getColor(isDarkMode ? R.color.fan_curve_dark_curve : R.color.fan_curve_light_curve);
        int fillColor = getColor(isDarkMode ? R.color.fan_curve_dark_fill : R.color.fan_curve_light_fill);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(bgColor);
        backgroundPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(gridColor);
        gridPaint.setStrokeWidth(1.5f * density);
        gridPaint.setStyle(Paint.Style.STROKE);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(axisColor);
        axisPaint.setStrokeWidth(2.5f * density);
        axisPaint.setStyle(Paint.Style.STROKE);

        curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curvePaint.setColor(curveColor);
        curvePaint.setStrokeWidth(3.5f * density);
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeCap(Paint.Cap.ROUND);
        curvePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(fillColor);
        fillPaint.setStyle(Paint.Style.FILL);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(pointColor);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setStrokeWidth(2.5f * density);
        if (!isDarkMode) {
            pointPaint.setShadowLayer(3 * density, 0, 0, Color.parseColor("#30000000"));
        }

        selectedPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPointPaint.setColor(curveColor);
        selectedPointPaint.setStyle(Paint.Style.FILL);
        if (!isDarkMode) {
            selectedPointPaint.setShadowLayer(5 * density, 0, 0, Color.parseColor("#60000000"));
        }

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(10 * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateGraphArea();
    }

    private void calculateGraphArea() {
        int availableWidth = getWidth() - paddingLeft - paddingRight;
        int availableHeight = getHeight() - paddingTop - paddingBottom;

        graphArea = new RectF(
                paddingLeft,
                paddingTop,
                paddingLeft + availableWidth,
                paddingTop + availableHeight
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (graphArea == null) {
            calculateGraphArea();
        }

        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        drawGrid(canvas);

        drawAxes(canvas);

        drawAxisLabels(canvas);

        drawCurveFill(canvas);

        drawCurve(canvas);

        drawPoints(canvas);
    }

    private void drawGrid(Canvas canvas) {
        for (int temp = MIN_TEMP; temp <= MAX_TEMP; temp += 10) {
            float x = tempToX(temp);
            canvas.drawLine(x, graphArea.top, x, graphArea.bottom, gridPaint);
        }

        for (int fan = 0; fan <= MAX_FAN; fan += 10) {
            float y = fanToY(fan);
            canvas.drawLine(graphArea.left, y, graphArea.right, y, gridPaint);
        }
    }

    private void drawAxes(Canvas canvas) {
        canvas.drawLine(graphArea.left, graphArea.top, graphArea.left, graphArea.bottom, axisPaint);

        canvas.drawLine(graphArea.left, graphArea.bottom, graphArea.right, graphArea.bottom, axisPaint);
    }

    private void drawAxisLabels(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.RIGHT);

        for (int fan = 0; fan <= MAX_FAN; fan += 10) {
            float y = fanToY(fan);
            canvas.drawText(fan + "%", paddingLeft - 6, y + 3, textPaint);
        }

        textPaint.setTextAlign(Paint.Align.CENTER);

        for (int temp = MIN_TEMP; temp <= MAX_TEMP; temp += 10) {
            float x = tempToX(temp);
            canvas.drawText(temp + "°", x, graphArea.bottom + paddingBottom - 6, textPaint);
        }
    }

    private void drawCurveFill(Canvas canvas) {
        if (points.size() < 1) return;

        canvas.save();

        Path clipPath = new Path();
        Preset.TempPoint first = points.get(0);

        clipPath.moveTo(tempToX(MIN_TEMP), fanToY(first.fanPercent));
        clipPath.lineTo(tempToX(first.temperature), fanToY(first.fanPercent));

        for (int i = 1; i < points.size(); i++) {
            Preset.TempPoint point = points.get(i);
            clipPath.lineTo(tempToX(point.temperature), fanToY(point.fanPercent));
        }

        Preset.TempPoint last = points.get(points.size() - 1);
        clipPath.lineTo(tempToX(MAX_TEMP), fanToY(last.fanPercent));

        clipPath.lineTo(tempToX(MAX_TEMP), graphArea.bottom);

        clipPath.lineTo(tempToX(MIN_TEMP), graphArea.bottom);

        clipPath.close();

        canvas.clipPath(clipPath);

        canvas.drawRect(graphArea.left, graphArea.top, graphArea.right, graphArea.bottom, fillPaint);

        canvas.restore();
    }

    private void drawCurve(Canvas canvas) {
        if (points.size() < 1) return;

        Path path = new Path();
        Preset.TempPoint first = points.get(0);

        path.moveTo(tempToX(MIN_TEMP), fanToY(first.fanPercent));
        path.lineTo(tempToX(first.temperature), fanToY(first.fanPercent));

        for (int i = 1; i < points.size(); i++) {
            Preset.TempPoint point = points.get(i);
            path.lineTo(tempToX(point.temperature), fanToY(point.fanPercent));
        }

        Preset.TempPoint last = points.get(points.size() - 1);
        path.lineTo(tempToX(MAX_TEMP), fanToY(last.fanPercent));

        canvas.drawPath(path, curvePaint);
    }

    private void drawPoints(Canvas canvas) {
        pointPaint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < points.size(); i++) {
            Preset.TempPoint point = points.get(i);
            float x = tempToX(point.temperature);
            float y = fanToY(point.fanPercent);
            canvas.drawCircle(x, y, pointRadius, pointPaint);
        }

        pointPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < points.size(); i++) {
            Preset.TempPoint point = points.get(i);
            float x = tempToX(point.temperature);
            float y = fanToY(point.fanPercent);

            if (i == selectedPointIndex) {
                canvas.drawCircle(x, y, selectedPointRadius, selectedPointPaint);
                pointPaint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(x, y, selectedPointRadius, pointPaint);
                pointPaint.setStyle(Paint.Style.FILL);
            } else {
                canvas.drawCircle(x, y, pointRadius - 2, pointPaint);
            }
        }
    }

    private float tempToX(int temp) {
        float ratio = (float) (temp - MIN_TEMP) / (MAX_TEMP - MIN_TEMP);
        return graphArea.left + ratio * graphArea.width();
    }

    private int xToTemp(float x) {
        float ratio = (x - graphArea.left) / graphArea.width();
        return MIN_TEMP + (int) (ratio * (MAX_TEMP - MIN_TEMP));
    }

    private float fanToY(int fanPercent) {
        float ratio = (float) fanPercent / MAX_FAN;
        return graphArea.bottom - ratio * graphArea.height();
    }

    private int yToFan(float y) {
        float ratio = (graphArea.bottom - y) / graphArea.height();
        return Math.round(ratio * MAX_FAN);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        ViewParent parent = getParent();
        if (parent != null) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || isDragging) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleActionUp();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleActionDown(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        selectedPointIndex = findNearestPointIndex(x, y);
        if (selectedPointIndex != -1) {
            isDragging = true;
            hasMovedBeyondSlop = false;
            touchStartX = x;
            touchStartY = y;
            invalidate();
            if (onPointSelectedListener != null) {
                onPointSelectedListener.onPointSelected(selectedPointIndex);
            }
        }
    }

    private void handleActionMove(MotionEvent event) {
        if (!isDragging || selectedPointIndex == -1) return;

        float x = event.getX();
        float y = event.getY();

        if (!hasMovedBeyondSlop) {
            float dx = x - touchStartX;
            float dy = y - touchStartY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < touchSlop) {
                return;
            }
            hasMovedBeyondSlop = true;
        }

        x = Math.max(graphArea.left, Math.min(graphArea.right, x));
        y = Math.max(graphArea.top, Math.min(graphArea.bottom, y));

        int newTemp = xToTemp(x);
        int newFan = yToFan(y);

        newFan = Math.max(MIN_FAN, Math.min(MAX_FAN, newFan));

        int minTemp = (selectedPointIndex > 0) ?
                points.get(selectedPointIndex - 1).temperature + 1 : MIN_TEMP;
        int maxTemp = (selectedPointIndex < points.size() - 1) ?
                points.get(selectedPointIndex + 1).temperature - 1 : MAX_TEMP;
        newTemp = Math.max(minTemp, Math.min(maxTemp, newTemp));

        int minFan = (selectedPointIndex > 0) ? points.get(selectedPointIndex - 1).fanPercent : MIN_FAN;
        newFan = Math.max(minFan, newFan);

        points.get(selectedPointIndex).temperature = newTemp;
        points.get(selectedPointIndex).fanPercent = newFan;

        for (int i = selectedPointIndex + 1; i < points.size(); i++) {
            if (points.get(i).fanPercent < newFan) {
                points.get(i).fanPercent = newFan;
            } else {
                break;
            }
        }

        invalidate();

        if (onPointChangedListener != null) {
            onPointChangedListener.onPointChanged(selectedPointIndex, newTemp, newFan);
        }
    }

    private void handleActionUp() {
        isDragging = false;
        hasMovedBeyondSlop = false;
        if (onPointSelectedListener != null) {
            onPointSelectedListener.onPointDeselected();
        }
        selectedPointIndex = -1;
        invalidate();
    }

    private int findNearestPointIndex(float x, float y) {
        int nearestIndex = -1;
        float minDistance = TOUCH_TOLERANCE_DP * getResources().getDisplayMetrics().density;

        for (int i = 0; i < points.size(); i++) {
            Preset.TempPoint point = points.get(i);
            float px = tempToX(point.temperature);
            float py = fanToY(point.fanPercent);
            float distance = (float) Math.sqrt(Math.pow(x - px, 2) + Math.pow(y - py, 2));

            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    public void setPoints(List<Preset.TempPoint> points) {
        this.points = new ArrayList<>(points);
        Collections.sort(this.points, Comparator.comparingInt(Preset.TempPoint::getTemperature));
        invalidate();
    }

    public List<Preset.TempPoint> getPoints() {
        return new ArrayList<>(points);
    }

    public void setOnPointChangedListener(OnPointChangedListener listener) {
        this.onPointChangedListener = listener;
    }

    public void setOnPointSelectedListener(OnPointSelectedListener listener) {
        this.onPointSelectedListener = listener;
    }

    public int getSelectedPointIndex() {
        return selectedPointIndex;
    }

    public interface OnPointChangedListener {
        void onPointChanged(int index, int temperature, int fanPercent);
    }

    public interface OnPointSelectedListener {
        void onPointSelected(int index);
        void onPointDeselected();
    }

    public OnPointChangedListener onPointChangedListener;
    private OnPointSelectedListener onPointSelectedListener;
}
