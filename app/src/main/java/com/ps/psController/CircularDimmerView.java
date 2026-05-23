package com.ps.psController;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

public class CircularDimmerView extends View {
    private Paint basePaint, progressPaint, thumbPaint, textPaint;
    private RectF rectF;
    private float centerX, centerY, radius;
    private float angle = 0; // 0 to 360
    private int progress = 0; // 0 to 100
    private DimmerListener listener;

    public interface DimmerListener {
        void onProgressChanged(int progress);
    }

    public CircularDimmerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        basePaint.setColor(Color.parseColor("#EEEEEE"));
        basePaint.setStyle(Paint.Style.STROKE);
        basePaint.setStrokeWidth(40);
        basePaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.parseColor("#FFB300")); // Dimmer color
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(40);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setColor(Color.WHITE);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setShadowLayer(10, 0, 0, Color.LTGRAY);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(100);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        rectF = new RectF();
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) / 2.5f;
        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw base circle
        canvas.drawArc(rectF, 0, 360, false, basePaint);

        // Draw progress arc (start from -90 to be at top)
        canvas.drawArc(rectF, -90, angle, false, progressPaint);

        // Draw progress text
        canvas.drawText(progress + "%", centerX, centerY + 35, textPaint);

        // Draw thumb
        double thumbAngle = Math.toRadians(angle - 90);
        float thumbX = (float) (centerX + radius * Math.cos(thumbAngle));
        float thumbY = (float) (centerY + radius * Math.sin(thumbAngle));
        canvas.drawCircle(thumbX, thumbY, 30, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateAngle(x, y);
                break;
            case MotionEvent.ACTION_UP:
                performClick();
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void updateAngle(float x, float y) {
        double dx = x - centerX;
        double dy = y - centerY;
        double theta = Math.toDegrees(Math.atan2(dy, dx));

        theta += 90; // Align with arc start
        if (theta < 0) theta += 360;

        int newProgress = Math.round(((float) theta / 360f) * 100);

        // Prevent wrapping: stop at 100% and 0%
        // If the jump is too large (e.g., from 95% to 5%), it means the user is trying to wrap around
        if (Math.abs(newProgress - progress) > 80) {
            return;
        }

        angle = (float) theta;
        progress = newProgress;

        if (listener != null) {
            listener.onProgressChanged(progress);
        }

        // Add subtle haptic feedback every 5%
        if (progress % 5 == 0) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }

        invalidate();
    }

    public void setDimmerListener(DimmerListener listener) {
        this.listener = listener;
    }
}