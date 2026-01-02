package com.seilent.rpfanctl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
    // Data Model
    private List<Preset.TempPoint> points;

    // Axis Configuration
    private static final int MIN_TEMP = 0;
    private static final int MAX_TEMP = 100;
    private static final int MIN_FAN = 0;
    private static final int MAX_FAN = 100;

    // Drawing Dimensions
    private RectF graphArea;
    private int paddingLeft, paddingTop, paddingRight, paddingBottom;

    // Touch Handling
    private int selectedPointIndex = -1;
    private boolean isDragging = false;
    private boolean hasMovedBeyondSlop;
    private float touchSlop;
    private float touchStartX, touchStartY;
    private static final int TOUCH_TOLERANCE_DP = 60;

    // Visual Configuration
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

    // Dark mode
    private boolean isDarkMode;

    // Colors for light mode
    private static final int LIGHT_BG = Color.parseColor("#F5F5F5");
    private static final int LIGHT_GRID = Color.parseColor("#E0E0E0");
    private static final int LIGHT_AXIS = Color.parseColor("#666666");
    private static final int LIGHT_TEXT = Color.parseColor("#666666");
    private static final int LIGHT_POINT = Color.WHITE;
    private static final int LIGHT_CURVE = Color.parseColor("#2196F3");
    private static final int LIGHT_FILL = Color.parseColor("#402196F3");

    // Colors for dark mode
    private static final int DARK_BG = Color.parseColor("#1E1E1E");
    private static final int DARK_GRID = Color.parseColor("#333333");
    private static final int DARK_AXIS = Color.parseColor("#888888");
    private static final int DARK_TEXT = Color.parseColor("#AAAAAA");
    private static final int DARK_POINT = Color.parseColor("#E0E0E0");
    private static final int DARK_CURVE = Color.parseColor("#4FC3F7");
    private static final int DARK_FILL = Color.parseColor("#504FC3F7");

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

        // Detect dark mode
        isDarkMode = isDarkMode();

        // Set default points
        points.add(new Preset.TempPoint(20, 0));
        points.add(new Preset.TempPoint(50, 10));
        points.add(new Preset.TempPoint(70, 15));
        points.add(new Preset.TempPoint(80, 20));

        // Calculate padding based on screen density
        float density = getResources().getDisplayMetrics().density;
        paddingLeft = (int) (45 * density);
        paddingRight = (int) (15 * density);
        paddingTop = (int) (30 * density);
        paddingBottom = (int) (35 * density);

        // Touch targets
        pointRadius = (int) (16 * density);
        selectedPointRadius = (int) (20 * density);

        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        // Enable touch interaction
        setClickable(true);
        setFocusable(true);

        // Initialize paints
        initPaints(density);
    }

    private boolean isDarkMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getResources().getConfiguration().isNightModeActive();
        }
        // Fallback for older SDKs
        int nightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void initPaints(float density) {
        int bgColor = isDarkMode ? DARK_BG : LIGHT_BG;
        int gridColor = isDarkMode ? DARK_GRID : LIGHT_GRID;
        int axisColor = isDarkMode ? DARK_AXIS : LIGHT_AXIS;
        int textColor = isDarkMode ? DARK_TEXT : LIGHT_TEXT;
        int pointColor = isDarkMode ? DARK_POINT : LIGHT_POINT;
        int curveColor = isDarkMode ? DARK_CURVE : LIGHT_CURVE;
        int fillColor = isDarkMode ? DARK_FILL : LIGHT_FILL;

        // Background
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(bgColor);
        backgroundPaint.setStyle(Paint.Style.FILL);

        // Grid
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(gridColor);
        gridPaint.setStrokeWidth(1.5f * density);
        gridPaint.setStyle(Paint.Style.STROKE);

        // Axis
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(axisColor);
        axisPaint.setStrokeWidth(2.5f * density);
        axisPaint.setStyle(Paint.Style.STROKE);

        // Curve
        curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curvePaint.setColor(curveColor);
        curvePaint.setStrokeWidth(3.5f * density);
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeCap(Paint.Cap.ROUND);
        curvePaint.setStrokeJoin(Paint.Join.ROUND);

        // Fill under curve
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(fillColor);
        fillPaint.setStyle(Paint.Style.FILL);

        // Point
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(pointColor);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setStrokeWidth(2.5f * density);
        if (!isDarkMode) {
            pointPaint.setShadowLayer(3 * density, 0, 0, Color.parseColor("#30000000"));
        }

        // Selected Point
        selectedPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPointPaint.setColor(curveColor);
        selectedPointPaint.setStyle(Paint.Style.FILL);
        if (!isDarkMode) {
            selectedPointPaint.setShadowLayer(5 * density, 0, 0, Color.parseColor("#60000000"));
        }

        // Text
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

        // Draw background
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        // Draw grid
        drawGrid(canvas);

        // Draw axes
        drawAxes(canvas);

        // Draw axis labels
        drawAxisLabels(canvas);

        // Draw fill under curve
        drawCurveFill(canvas);

        // Draw curve line
        drawCurve(canvas);

        // Draw points
        drawPoints(canvas);
    }

    private void drawGrid(Canvas canvas) {
        // Vertical lines (temperature) - every 10°C
        for (int temp = MIN_TEMP; temp <= MAX_TEMP; temp += 10) {
            float x = tempToX(temp);
            canvas.drawLine(x, graphArea.top, x, graphArea.bottom, gridPaint);
        }

        // Horizontal lines (fan %) - every 25%
        for (int fan = 0; fan <= MAX_FAN; fan += 25) {
            float y = fanToY(fan);
            canvas.drawLine(graphArea.left, y, graphArea.right, y, gridPaint);
        }
    }

    private void drawAxes(Canvas canvas) {
        // Left axis (Y)
        canvas.drawLine(graphArea.left, graphArea.top, graphArea.left, graphArea.bottom, axisPaint);

        // Bottom axis (X)
        canvas.drawLine(graphArea.left, graphArea.bottom, graphArea.right, graphArea.bottom, axisPaint);
    }

    private void drawAxisLabels(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.RIGHT);

        // Y-axis labels (fan %)
        for (int fan = 0; fan <= MAX_FAN; fan += 25) {
            float y = fanToY(fan);
            canvas.drawText(fan + "%", paddingLeft - 6, y + 3, textPaint);
        }

        textPaint.setTextAlign(Paint.Align.CENTER);

        // X-axis labels (temperature)
        for (int temp = MIN_TEMP; temp <= MAX_TEMP; temp += 10) {
            float x = tempToX(temp);
            canvas.drawText(temp + "°", x, graphArea.bottom + paddingBottom - 6, textPaint);
        }
    }

    private void drawCurveFill(Canvas canvas) {
        if (points.size() < 2) return;

        Path path = new Path();
        Preset.TempPoint first = points.get(0);
        path.moveTo(tempToX(first.temperature), fanToY(first.fanPercent));

        for (int i = 1; i < points.size(); i++) {
            Preset.TempPoint point = points.get(i);
            path.lineTo(tempToX(point.temperature), fanToY(point.fanPercent));
        }

        // Close path to bottom for fill
        path.lineTo(tempToX(points.get(points.size() - 1).temperature), graphArea.bottom);
        path.lineTo(tempToX(points.get(0).temperature), graphArea.bottom);
        path.close();

        canvas.drawPath(path, fillPaint);
    }

    private void drawCurve(Canvas canvas) {
        if (points.size() < 2) return;

        Path path = new Path();
        Preset.TempPoint first = points.get(0);
        path.moveTo(tempToX(first.temperature), fanToY(first.fanPercent));

        for (int i = 1; i < points.size(); i++) {
            Preset.TempPoint point = points.get(i);
            path.lineTo(tempToX(point.temperature), fanToY(point.fanPercent));
        }

        canvas.drawPath(path, curvePaint);
    }

    private void drawPoints(Canvas canvas) {
        // Draw border for all points
        pointPaint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < points.size(); i++) {
            Preset.TempPoint point = points.get(i);
            float x = tempToX(point.temperature);
            float y = fanToY(point.fanPercent);
            canvas.drawCircle(x, y, pointRadius, pointPaint);
        }

        // Fill points based on selection
        pointPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < points.size(); i++) {
            Preset.TempPoint point = points.get(i);
            float x = tempToX(point.temperature);
            float y = fanToY(point.fanPercent);

            if (i == selectedPointIndex) {
                canvas.drawCircle(x, y, selectedPointRadius, selectedPointPaint);
                // Draw border around selected point
                pointPaint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(x, y, selectedPointRadius, pointPaint);
                pointPaint.setStyle(Paint.Style.FILL);
            } else {
                canvas.drawCircle(x, y, pointRadius - 2, pointPaint);
            }
        }
    }

    // Coordinate transformation
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
        // Prevent parent from intercepting touch events when dragging points
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
        }
    }

    private void handleActionMove(MotionEvent event) {
        if (!isDragging || selectedPointIndex == -1) return;

        float x = event.getX();
        float y = event.getY();

        // Apply touch slop only at the start of dragging
        if (!hasMovedBeyondSlop) {
            float dx = x - touchStartX;
            float dy = y - touchStartY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < touchSlop) {
                return; // Still within touch slop, ignore
            }
            hasMovedBeyondSlop = true;
        }

        // Constrain to graph area
        x = Math.max(graphArea.left, Math.min(graphArea.right, x));
        y = Math.max(graphArea.top, Math.min(graphArea.bottom, y));

        // Convert to temp/fan values
        int newTemp = xToTemp(x);
        int newFan = yToFan(y);

        // Clamp fan percentage
        newFan = Math.max(MIN_FAN, Math.min(MAX_FAN, newFan));

        // Enforce ascending temperature order
        int minTemp = (selectedPointIndex > 0) ?
                points.get(selectedPointIndex - 1).temperature + 1 : MIN_TEMP;
        int maxTemp = (selectedPointIndex < points.size() - 1) ?
                points.get(selectedPointIndex + 1).temperature - 1 : MAX_TEMP;
        newTemp = Math.max(minTemp, Math.min(maxTemp, newTemp));

        // Update point
        points.get(selectedPointIndex).temperature = newTemp;
        points.get(selectedPointIndex).fanPercent = newFan;

        invalidate();

        // Notify listener
        if (onPointChangedListener != null) {
            onPointChangedListener.onPointChanged(selectedPointIndex, newTemp, newFan);
        }
    }

    private void handleActionUp() {
        isDragging = false;
        hasMovedBeyondSlop = false;
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

    // Public API
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

    public interface OnPointChangedListener {
        void onPointChanged(int index, int temperature, int fanPercent);
    }

    public OnPointChangedListener onPointChangedListener;
}
