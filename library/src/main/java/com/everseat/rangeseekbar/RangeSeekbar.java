package com.everseat.rangeseekbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;

import static com.everseat.rangeseekbar.Util.dpToPx;
import static com.everseat.rangeseekbar.Util.expandRect;
import static com.everseat.rangeseekbar.Util.withinBounds;

/**
 * A seek bar with a configurable min/max range values.
 */
public class RangeSeekbar extends AbsSeekbar {
  private Drawable leftThumbDrawable;
  private Drawable rightThumbDrawable;

  // Size holders
  private Rect sharedTextBounds = new Rect();
  private Rect leftThumbBounds = new Rect();
  private Rect rightThumbBounds = new Rect();

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
    leftThumbDrawable = getThumbDrawable();
    rightThumbDrawable = leftThumbDrawable.getConstantState().newDrawable();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = MotionEventCompat.getActionMasked(event);
    switch (action) {
      case MotionEvent.ACTION_DOWN: return handleDownEvent(event);
      case MotionEvent.ACTION_MOVE: return handleMoveEvent(event);
      case MotionEvent.ACTION_UP: return handleUpEvent();
      default: return super.onTouchEvent(event);
    }
  }

  private boolean handleUpEvent() {
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
  }

  private boolean handleMoveEvent(MotionEvent event) {
    // We only support a horizontal bar at the moment
    int radius = leftThumbBounds.width() / 2;
    float newX = (int) event.getX() - radius;

    if (activeThumb == THUMB_LEFT) {
      Rect destination = rightThumbBounds;

      setRectXPosition(leftThumbBounds, (int) newX);

      // Ensure left thumb does not cross paths with right thumb
      if (leftThumbBounds.right >= destination.left) {
        setRectXPosition(leftThumbBounds, destination.left - leftThumbBounds.width());
        minValue = calculateValue((int) (leftThumbBounds.centerX() - getTrackBounds().left));
      } else {
        // Ensure left thumb doesn't get moved outside of bounds
        if (leftThumbBounds.centerX() <= getTrackBounds().left) {
          setRectXPosition(leftThumbBounds, (int) (getTrackBounds().left - (leftThumbBounds.width()/2)));
        }

        // We want the center of the thumb drawable to be the deciding factor
        minValue = Math.max(calculateValue((int) (leftThumbBounds.centerX() - getTrackBounds().left)), 0);
      }
    }

    if (activeThumb == THUMB_RIGHT) {
      Rect destination = leftThumbBounds;

      setRectXPosition(rightThumbBounds, (int) newX);

      // Ensure right thumb does not cross paths with left thumb
      if (rightThumbBounds.left <= destination.right) {
        setRectXPosition(rightThumbBounds, destination.right);
        maxValue = calculateValue((int) (rightThumbBounds.centerX() - getTrackBounds().left));
      } else {
        // Ensure right thumb doesn't get moved outside of bounds
        if (rightThumbBounds.centerX() >= getTrackBounds().right) {
          setRectXPosition(rightThumbBounds, (int) (getTrackBounds().right - (rightThumbBounds.width()/2)));
        }

        maxValue = Math.min(calculateValue((int) (rightThumbBounds.centerX() - getTrackBounds().left)), 1);
      }
    }

    invalidate();
    return true;
  }

  private boolean handleDownEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();

    if (withinBounds(x, y, expandRect(leftThumbBounds, (int) dpToPx(getResources(), 4)))) {
      activeThumb = THUMB_LEFT;
      if (getParent() != null) {
        getParent().requestDisallowInterceptTouchEvent(true);
      }
      invalidate();
      return true;
    }

    if (withinBounds(x, y, expandRect(rightThumbBounds, (int) dpToPx(getResources(), 4)))) {
      activeThumb = THUMB_RIGHT;
      if (getParent() != null) {
        getParent().requestDisallowInterceptTouchEvent(true);
      }
      invalidate();
      return true;
    }

    return false;
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    RectF trackBounds = getTrackBounds();

    // Right thumb
    int rightThumbSize = Math.max(rightThumbDrawable.getIntrinsicWidth(), rightThumbDrawable.getIntrinsicHeight());
    rightThumbBounds.set(rightThumbBounds.left, rightThumbBounds.top, rightThumbBounds.left + rightThumbSize, leftThumbBounds.top + rightThumbSize);
    int centerX = (int) (trackBounds.left + trackBounds.width() * maxValue);
    int centerY = (int) trackBounds.centerY();
    setRectCenterX(rightThumbBounds, centerX);
    setRectCenterY(rightThumbBounds, centerY);

    // Left thumb
    int leftThumbSize = Math.max(leftThumbDrawable.getIntrinsicWidth(), leftThumbDrawable.getIntrinsicHeight());
    leftThumbBounds.set(leftThumbBounds.left, leftThumbBounds.top, leftThumbBounds.left + leftThumbSize, leftThumbBounds.top + leftThumbSize);
    centerX = (int) (trackBounds.left + trackBounds.width() * minValue);
    centerY = (int) trackBounds.centerY();
    setRectCenterX(leftThumbBounds, centerX);
    setRectCenterY(leftThumbBounds, centerY);

  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    onDrawThumb(canvas);

    // Draw left value text
    String minValueText = formatValue(minValue);
    measureText(minValueText, sharedTextBounds);
    setRectXPosition(sharedTextBounds, leftThumbBounds.centerX() - (sharedTextBounds.width() / 2));
    setRectYPosition(sharedTextBounds, leftThumbBounds.bottom + getValueTextPadding());
    drawValueText(canvas, minValueText, sharedTextBounds);

    // Draw right value text
    String maxValueText = formatValue(maxValue);
    measureText(maxValueText, sharedTextBounds);
    setRectXPosition(sharedTextBounds, rightThumbBounds.centerX() - (sharedTextBounds.width() / 2));
    setRectYPosition(sharedTextBounds, rightThumbBounds.bottom + getValueTextPadding());
    drawValueText(canvas, maxValueText, sharedTextBounds);
  }

  private void onDrawThumb(Canvas canvas) {
    // Right
    rightThumbDrawable.setBounds(rightThumbBounds);
    rightThumbDrawable.setState(activeThumb == THUMB_RIGHT ? STATE_PRESSED : STATE_DEFAULT);
    rightThumbDrawable.draw(canvas);

    // Left
    leftThumbDrawable.setBounds(leftThumbBounds);
    leftThumbDrawable.setState(activeThumb == THUMB_LEFT ? STATE_PRESSED : STATE_DEFAULT);
    leftThumbDrawable.draw(canvas);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Public API
  //////////////////////////////////////////////////////////////////////////////////////////////////

  public void setCurrentRange(float min, float max) {
    minValue = min;
    maxValue = max;

    if (!ViewCompat.isInLayout(this)) {
      requestLayout();
      invalidate();
    }
  }

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
        getTrackBounds().top,
        rightThumbBounds.centerX(),
        getTrackBounds().bottom);
    canvas.drawRoundRect(fill, getTrackHeight() / 2, getTrackHeight() / 2, sharedPaint);
  }

  public interface OnValueSetListener {
    void onMinValueSet(float value);
    void onMaxValueSet(float value);
  }
}
