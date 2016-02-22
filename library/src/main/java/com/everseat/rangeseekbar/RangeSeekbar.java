package com.everseat.rangeseekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A seek bar with a configurable min/max range values
 */
public class RangeSeekbar extends AbsSeekbar {
  private Drawable thumbDrawable;

  // Dimensions
  private int valueTextPadding = 0;
  private int thumbSize = 0;

  // Size holders
  private Rect sharedTextBounds = new Rect();
  private RectF trackBounds = new RectF();
  private Rect leftThumbBounds = new Rect();
  private Rect rightThumbBounds = new Rect();

  // Paint
  private Paint valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

  // State values
  private int activeThumb = -1;
  private float minValue = 0f;
  private float maxValue = 1f;

  @Nullable private OnValueSetListener valueSetListener;

  private static final int THUMB_LEFT = 0;
  private static final int THUMB_RIGHT = 1;
  private static final int[] STATE_PRESSED = new int[] {android.R.attr.state_pressed};
  private static final int[] STATE_DEFAULT = new int[] {};

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
    int valueTextSize = 0;
    int valueTextPadding = 0;
    Drawable thumbDrawable = null;

    if (attrs != null) {
      TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekbar);

      int count = ta.getIndexCount();
      for (int i = 0; i < count; i++) {
        int attr = ta.getIndex(i);
        if (attr == R.styleable.RangeSeekbar_valueTextSize) {
          valueTextSize = ta.getInt(attr, (int) dpToPx(14));
        } else if (attr == R.styleable.RangeSeekbar_valueTextPadding) {
          valueTextPadding = ta.getInt(attr, (int) dpToPx(4));
        } else if (attr == R.styleable.RangeSeekbar_thumbDrawable) {
          thumbDrawable = ta.getDrawable(attr);
        }
      }

      ta.recycle();
    }

    if (valueTextSize == 0) {
      valueTextSize = (int) dpToPx(14);
    }
    if (valueTextPadding == 0) {
      valueTextPadding = (int) dpToPx(4);
    }
    if (thumbDrawable == null) {
      thumbDrawable = getResources().getDrawable(R.drawable.ic_thumb_seekbar);
    }

    setValueTextSize(valueTextSize);
    setValueTextPadding(valueTextPadding);
    setThumbDrawable(thumbDrawable);
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
        }
        if (withinBounds(x, y, rightThumbBounds)) {
          activeThumb = THUMB_RIGHT;
        }

        if (activeThumb != -1) {
          if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
          }
          invalidate();
          return true;
        }

        return false;
      case MotionEvent.ACTION_MOVE:
        // We only support a horizontal bar at the moment
        int radius = leftThumbBounds.width() / 2;
        float newX = (int) event.getX() - radius;

        if (activeThumb == THUMB_LEFT) {
          Rect destination = rightThumbBounds;
          if (newX >= destination.left - leftThumbBounds.width()) {
            newX = destination.left - leftThumbBounds.width();
          }

          setRectXPosition(leftThumbBounds, (int) newX);
          if (leftThumbBounds.centerX() <= trackBounds.left) {
            setRectXPosition(leftThumbBounds, (int) (trackBounds.left - (leftThumbBounds.width()/2)));
          }

          // We want the center of the thumb drawable to be the deciding factor
          minValue = Math.max(calculateValue((int) (newX + radius - trackBounds.left)), 0);
        }

        if (activeThumb == THUMB_RIGHT) {
          Rect destination = leftThumbBounds;
          if (newX <= destination.right) {
            newX = destination.right;
          }

          setRectXPosition(rightThumbBounds, (int) newX);
          if (rightThumbBounds.centerX() >= trackBounds.right) {
            setRectXPosition(rightThumbBounds, (int) (trackBounds.right - (rightThumbBounds.width()/2)));
          }

          // We want the center of the thumb drawable to be the deciding factor
          maxValue = Math.min(calculateValue((int) (newX + radius - trackBounds.left)), 1);
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

    // Left thumb
    int x = (int) (trackBounds.left - (thumbSize / 2));
    int y = (int) (trackBounds.centerY() - (thumbSize / 2));
    leftThumbBounds.set(x, y, x + thumbSize, y + thumbSize);

    // Right thumb
    x = (int) (trackBounds.right - (thumbSize / 2));
    rightThumbBounds.set(x, y, x + thumbSize, y + thumbSize);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    thumbDrawable.setBounds(leftThumbBounds);
    thumbDrawable.setState(activeThumb == THUMB_LEFT ? STATE_PRESSED : STATE_DEFAULT);
    thumbDrawable.draw(canvas);

    thumbDrawable.setBounds(rightThumbBounds);
    thumbDrawable.setState(activeThumb == THUMB_RIGHT ? STATE_PRESSED : STATE_DEFAULT);
    thumbDrawable.draw(canvas);

    // Draw min value text
    String minValueText = formatValue(minValue);
    valuePaint.getTextBounds(minValueText, 0, minValueText.length(), sharedTextBounds);
    setRectXPosition(sharedTextBounds, leftThumbBounds.centerX() - (sharedTextBounds.width() / 2));
    setRectYPosition(sharedTextBounds, leftThumbBounds.bottom + valueTextPadding);
    drawValue(canvas, minValueText, sharedTextBounds);

    // Draw max value text
    String maxValueText = formatValue(maxValue);
    valuePaint.getTextBounds(maxValueText, 0, maxValueText.length(), sharedTextBounds);
    setRectXPosition(sharedTextBounds, rightThumbBounds.centerX() - (sharedTextBounds.width() / 2));
    setRectYPosition(sharedTextBounds, rightThumbBounds.bottom + valueTextPadding);
    drawValue(canvas, maxValueText, sharedTextBounds);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Public API
  //////////////////////////////////////////////////////////////////////////////////////////////////

  public void setThumbDrawable(@DrawableRes int drawable) {
    setThumbDrawable(getResources().getDrawable(drawable));
  }

  public void setThumbDrawable(Drawable drawable) {
    if (drawable == null) return;
    thumbDrawable = drawable;
    thumbSize = Math.max(thumbDrawable.getIntrinsicHeight(), thumbDrawable.getIntrinsicWidth());
  }

  public void setValueTextPadding(int paddingInPx) {
    valueTextPadding = paddingInPx;
  }

  public void setValueTextSize(int textSize) {
    valuePaint.setTextSize(textSize);
  }

  public void setOnValueSetListener(@Nullable OnValueSetListener valueSetListener) {
    this.valueSetListener = valueSetListener;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Internal methods
  //////////////////////////////////////////////////////////////////////////////////////////////////

  private float calculateValue(int x) {
    return x / trackBounds.width();
  }

  private void drawValue(Canvas canvas, String text, Rect bounds) {
    canvas.drawText(text, bounds.left, bounds.bottom, valuePaint);
  }

  private boolean withinBounds(float x, float y, Rect bounds) {
    int padding = (int) dpToPx(4); // Add padding to augment touch target size
    return (x > bounds.left - padding && x < bounds.right + padding) &&
        (y > bounds.top - padding && y < bounds.bottom + padding);
  }

  @Override
  protected void onDrawTrackDecoration(Canvas canvas, Paint sharedPaint) {
    sharedPaint.reset();
    sharedPaint.setAntiAlias(true);
    sharedPaint.setColor(getTrackFillColor());
    RectF fill = new RectF(leftThumbBounds.centerX(),
        trackBounds.top,
        rightThumbBounds.centerX(),
        trackBounds.bottom);
    canvas.drawRoundRect(fill, getTrackHeight() / 2, getTrackHeight() / 2, sharedPaint);
  }

  @Override
  protected int getTrackLeftOffset() {
    return leftThumbBounds.width() / 2;
  }

  @Override
  protected int getTrackRightOffset() {
    return rightThumbBounds.width() / 2;
  }

  public interface OnValueSetListener {
    void onMinValueSet(float value);
    void onMaxValueSet(float value);
  }
}
