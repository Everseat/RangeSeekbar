package com.everseat.rangeseekbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;

import static com.everseat.rangeseekbar.Util.dpToPx;
import static com.everseat.rangeseekbar.Util.expandRect;
import static com.everseat.rangeseekbar.Util.withinBounds;

/**
 * A simple seek bar
 */
public class Seekbar extends AbsSeekbar {
  // Size holders
  private Rect thumbBounds = new Rect();

  // State values
  private float currentValue = 0f;

  @Nullable private OnValueSetListener valueSetListener;

  public Seekbar(Context context) {
    super(context);
  }

  public Seekbar(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public Seekbar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = MotionEventCompat.getActionMasked(event);
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        float x = event.getX();
        float y = event.getY();
        if (withinBounds(x, y, expandRect(thumbBounds, (int) dpToPx(getResources(), 4)))) {
          if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
          }

          getThumbDrawable().setState(STATE_PRESSED);
          invalidate();
          return true;
        }

        return false;
      case MotionEvent.ACTION_MOVE:
        // We only support a horizontal bar at the moment
        int radius = thumbBounds.width() / 2;
        float newX = (int) event.getX() - radius;

        setRectXPosition(thumbBounds, (int) newX);

        if (thumbBounds.centerX() >= getTrackBounds().right) {
          setRectXPosition(thumbBounds, (int) (getTrackBounds().right - (thumbBounds.width()/2)));
        }
        if (thumbBounds.centerX() <= getTrackBounds().left) {
          setRectXPosition(thumbBounds, (int) (getTrackBounds().left - (thumbBounds.width()/2)));
        }

        // We want the center of the thumb drawable to be the deciding factor
        currentValue = calculateValue((int) (newX + radius - getTrackBounds().left));

        invalidate();
        return true;
      case MotionEvent.ACTION_UP:
        // Notify OnValueSetListener
        if (valueSetListener != null) {
          valueSetListener.onValueSet(currentValue);
        }

        getThumbDrawable().setState(STATE_DEFAULT);
        invalidate();
        return true;
      default:
        return super.onTouchEvent(event);
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    RectF trackBounds = getTrackBounds();

    int thumbSize = Math.max(getThumbDrawable().getIntrinsicWidth(), getThumbDrawable().getIntrinsicHeight());
    float offset = currentValue * trackBounds.width();
    int x = (int) (trackBounds.left + offset) - (thumbSize / 2);
    int y = (int) (trackBounds.centerY() - (thumbSize / 2));
    thumbBounds.set(x, y, x + thumbSize, y + thumbSize);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    getThumbDrawable().setBounds(thumbBounds);
    getThumbDrawable().draw(canvas);

    // Draw current value text
    String text = formatValue(currentValue);
    measureText(text, sharedTextBounds);
    setRectXPosition(sharedTextBounds, thumbBounds.centerX() - (sharedTextBounds.width() / 2));
    setRectYPosition(sharedTextBounds, thumbBounds.bottom + getValueTextPadding());
    onDrawValueText(canvas, sharedTextBounds, text);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Public API
  //////////////////////////////////////////////////////////////////////////////////////////////////

  public void setOnValueSetListener(@Nullable OnValueSetListener valueSetListener) {
    this.valueSetListener = valueSetListener;
  }

  public void setCurrentValue(float value) {
    currentValue = value;
    if (!ViewCompat.isInLayout(this)) {
      requestLayout();
      invalidate();
    }
  }

  public float getCurrentValue() {
    return currentValue;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Internal methods
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  protected void onDrawTrackDecoration(Canvas canvas, Paint sharedPaint) {
    sharedPaint.reset();
    sharedPaint.setAntiAlias(true);
    sharedPaint.setColor(getTrackFillColor());
    RectF fill = new RectF(getTrackBounds().left,
        getTrackBounds().top,
        thumbBounds.centerX(),
        getTrackBounds().bottom);
    canvas.drawRoundRect(fill, getTrackHeight() / 2, getTrackHeight() / 2, sharedPaint);
  }

  protected void onDrawValueText(Canvas canvas, Rect textBounds, String text) {
    drawValueText(canvas, text, textBounds);
  }

  public interface OnValueSetListener {
    void onValueSet(float value);
  }
}
