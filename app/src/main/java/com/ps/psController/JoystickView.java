package com.ps.psController;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {
    private float centerX, centerY;
    private float baseRadius, stickRadius;
    private float stickX, stickY;
    private final Paint basePaint = new Paint();
    private final Paint stickPaint = new Paint();
    private JoystickListener joystickCallback;

    public interface JoystickListener {
        void onJoystickMoved(float xPercent, float yPercent, int id);
    }

    public JoystickView(Context context) {
        super(context);
        init();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        basePaint.setColor(Color.LTGRAY);
        basePaint.setStyle(Paint.Style.FILL);
        basePaint.setAntiAlias(true);

        stickPaint.setColor(Color.DKGRAY);
        stickPaint.setStyle(Paint.Style.FILL);
        stickPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        baseRadius = Math.min(w, h) / 3f;
        stickRadius = baseRadius / 2.5f;
        stickX = centerX;
        stickY = centerY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
        canvas.drawCircle(stickX, stickY, stickRadius, stickPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() != MotionEvent.ACTION_UP) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
            float absDistance = (float) Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
            if (absDistance <= baseRadius) {
                stickX = x;
                stickY = y;
            } else {
                float ratio = baseRadius / absDistance;
                stickX = centerX + (x - centerX) * ratio;
                stickY = centerY + (y - centerY) * ratio;
            }
        } else {
            stickX = centerX;
            stickY = centerY;
        }

        invalidate();

        if (joystickCallback != null) {
            float xPercent = (stickX - centerX) / baseRadius;
            float yPercent = (stickY - centerY) / baseRadius;
            joystickCallback.onJoystickMoved(xPercent, yPercent, getId());
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick();
        }

        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void setJoystickListener(JoystickListener listener) {
        this.joystickCallback = listener;
    }
}