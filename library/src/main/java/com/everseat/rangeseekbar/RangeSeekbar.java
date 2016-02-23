package com.everseat.rangeseekbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A seek bar with a configurable min/max range values.
 */
public class RangeSeekbar extends Seekbar {
  // Size holders
  private RectF trackBounds;
  private Rect leftThumbBounds = new Rect();
  private Rect rightThumbBounds; // This will be aliased to the existing thumbBounds from Seekbar

  // State values
  private int activeThumb = -1;
  private float minValue = 0f;
  private float maxValue = 1f;

  @Nullable private OnValueSetListener valueSetListener;

  private static final int THUMB_LEFT = 0;
  private static final int THUMB_RIGHT = 1;

  public RangeSeekbar(Context context) {
    super(context);
    init(context, null);
  }

  public RangeSeekbar(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public RangeSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    setCurrentValue(maxValue);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = MotionEventCompat.getActionMasked(event);
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        float x = event.getX();
        float y = event.getY();
        if (withinBounds(x, y, leftThumbBounds)) {
          activeThumb = THUMB_LEFT;
          if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
          }
          invalidate();
          return true;
        }

        boolean result = super.onTouchEvent(event);
        if (result) activeThumb = THUMB_RIGHT;
        return result;
      case MotionEvent.ACTION_MOVE:
        // We only support a horizontal bar at the moment
        int radius = leftThumbBounds.width() / 2;
        float newX = (int) event.getX() - radius;

        if (activeThumb == THUMB_LEFT) {
          Rect destination = rightThumbBounds;

          setRectXPosition(leftThumbBounds, (int) newX);

          // Ensure left thumb does not cross paths with right thumb
          if (leftThumbBounds.right >= destination.left) {
            setRectXPosition(leftThumbBounds, destination.left - leftThumbBounds.width());
            minValue = calculateValue((int) (leftThumbBounds.centerX() - trackBounds.left));
          } else {
            // Ensure left thumb doesn't get moved outside of bounds
            if (leftThumbBounds.centerX() <= trackBounds.left) {
              setRectXPosition(leftThumbBounds, (int) (trackBounds.left - (leftThumbBounds.width()/2)));
            }

            // We want the center of the thumb drawable to be the deciding factor
            minValue = Math.max(calculateValue((int) (leftThumbBounds.centerX() - trackBounds.left)), 0);
          }
        }

        if (activeThumb == THUMB_RIGHT) {
          Rect destination = leftThumbBounds;

          setRectXPosition(rightThumbBounds, (int) newX);

          // Ensure right thumb does not cross paths with left thumb
          if (rightThumbBounds.left <= destination.right) {
            setRectXPosition(rightThumbBounds, destination.right);
            maxValue = calculateValue((int) (rightThumbBounds.centerX() - trackBounds.left));
            setCurrentValue(maxValue);
          } else {
            // Ensure right thumb doesn't get moved outside of bounds
            if (rightThumbBounds.centerX() >= trackBounds.right) {
              setRectXPosition(rightThumbBounds, (int) (trackBounds.right - (rightThumbBounds.width()/2)));
            }

            maxValue = Math.min(calculateValue((int) (rightThumbBounds.centerX() - trackBounds.left)), 1);
            setCurrentValue(maxValue);
          }
        }

        invalidate();
        return true;
      case MotionEvent.ACTION_UP:
        // Notify OnValueSetListener
        if (valueSetListener != null) {
          if (activeThumb == THUMB_LEFT) {
            valueSetListener.onMinValueSet(minValue);
          } else if (activeThumb == THUMB_RIGHT) {
            valueSetListener.onMaxValueSet(maxValue);
          }
        }

        activeThumb = -1;
        invalidate();
        return true;
      default:
        return super.onTouchEvent(event);
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    trackBounds = getTrackBounds();
    Drawable thumbDrawable = getThumbDrawable();
    int thumbSize = thumbDrawable.getBounds().width();

    // Right thumb
    rightThumbBounds = getThumbBounds();

    // Left thumb
    int x = (int) (trackBounds.left - (thumbSize / 2));
    int y = (int) (trackBounds.centerY() - (thumbSize / 2));
    leftThumbBounds.set(x, y, x + thumbSize, y + thumbSize);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    // Draw left value text
    String minValueText = formatValue(minValue);
    Rect minValueTextBounds = measureValueText(minValueText);
    setRectXPosition(minValueTextBounds, leftThumbBounds.centerX() - (minValueTextBounds.width() / 2));
    setRectYPosition(minValueTextBounds, leftThumbBounds.bottom + getValueTextPadding());
    onDrawValueText(canvas, minValueTextBounds, minValueText);
  }

  @Override
  protected void onDrawThumb(Canvas canvas) {
    Drawable thumbDrawable = getThumbDrawable();

    // Right
    thumbDrawable.setBounds(rightThumbBounds);
    thumbDrawable.setState(activeThumb == THUMB_RIGHT ? STATE_PRESSED : STATE_DEFAULT);
    thumbDrawable.draw(canvas);

    // Left
    thumbDrawable.setBounds(leftThumbBounds);
    thumbDrawable.setState(activeThumb == THUMB_LEFT ? STATE_PRESSED : STATE_DEFAULT);
    thumbDrawable.draw(canvas);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Public API
  //////////////////////////////////////////////////////////////////////////////////////////////////

  public void setOnValueSetListener(@Nullable RangeSeekbar.OnValueSetListener valueSetListener) {
    this.valueSetListener = valueSetListener;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Internal methods
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  protected void onDrawTrackDecoration(Canvas canvas, Paint sharedPaint) {
    sharedPaint.reset();
    sharedPaint.setAntiAlias(true);
    sharedPaint.setColor(getTrackFillColor());
    RectF fill = new RectF(leftThumbBounds.centerX(),
        trackBounds.top,
        getThumbBounds().centerX(),
        trackBounds.bottom);
    canvas.drawRoundRect(fill, getTrackHeight() / 2, getTrackHeight() / 2, sharedPaint);
  }

  public interface OnValueSetListener {
    void onMinValueSet(float value);
    void onMaxValueSet(float value);
  }
}
