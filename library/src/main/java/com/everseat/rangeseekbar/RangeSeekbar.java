package com.everseat.rangeseekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.view.MotionEventCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * A seek bar with a configurable min/max range values
 */
public class RangeSeekbar extends View {
  private Drawable leftThumbDrawable;
  private Drawable rightThumbDrawable;
  private Drawable activeThumbDrawable;

  // Dimensions
  private int trackHeight = 0;
  private int valueTextPadding = 0;
  private int valueTextSize = 0;
  private int labelTextPadding = 0;
  private int labelTextSize = 0;
  private int thumbSize = 0;

  // Size holders
  private RectF trackBounds = new RectF();
  private Rect sharedTextBounds = new Rect();
  private Rect minValueLabelBounds = new Rect();
  private Rect maxValueLabelBounds = new Rect();

  // Paint
  private Paint valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
  private Paint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
  private Paint sharedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  // State values
  private int trackColor = 0;
  private int trackFillColor = 0;
  private float initialTouchX = 0;
  private float initialTouchedY = 0;
  private float minValue = 0f;
  private float maxValue = 1f;
  private String minLabelText;
  private String maxLabelText;

  private ValueFormatter valueFormatter = new ValueFormatter() {
    @Override
    public String formatValue(float value) {
      return String.valueOf(value);
    }
  };
  @Nullable private OnValueSetListener valueSetListener;

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
    applyAttributes(context, attrs);

    // Configure paint
    valuePaint.setTextSize(valueTextSize);
    labelPaint.setTextSize(labelTextSize);

    thumbSize = Math.max(leftThumbDrawable.getIntrinsicHeight(), leftThumbDrawable.getIntrinsicWidth());
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = MotionEventCompat.getActionMasked(event);
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        initialTouchX = event.getX();
        initialTouchedY = event.getY();
        if (withinBounds(initialTouchX, initialTouchedY, leftThumbDrawable.getBounds())) {
          activeThumbDrawable = leftThumbDrawable;
        }
        if (withinBounds(initialTouchX, initialTouchedY, rightThumbDrawable.getBounds())) {
          activeThumbDrawable = rightThumbDrawable;
        }

        if (activeThumbDrawable != null) {
          if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
          }
          activeThumbDrawable.setState(new int[] {android.R.attr.state_pressed});
          invalidate();
          return true;
        }

        return false;
      case MotionEvent.ACTION_MOVE:
        // We only support a horizontal bar at the moment
        Rect source = activeThumbDrawable.getBounds();
        int radius = source.width() / 2;
        float newX = (int) event.getX() - radius;

        if (activeThumbDrawable == leftThumbDrawable) {
          Rect destination = rightThumbDrawable.getBounds();
          if (newX >= destination.left - source.width()) {
            newX = destination.left - source.width();
          }

          // We want the center of the thumb drawable to be the deciding factor
          minValue = Math.max(calculateValue((int) (newX + radius - trackBounds.left)), 0);
        }

        if (activeThumbDrawable == rightThumbDrawable) {
          Rect destination = leftThumbDrawable.getBounds();
          if (newX <= destination.right) {
            newX = destination.right;
          }

          // We want the center of the thumb drawable to be the deciding factor
          maxValue = Math.min(calculateValue((int) (newX + radius - trackBounds.left)), 1);
        }

        moveDrawableHorizontally((int) newX, activeThumbDrawable);
        return true;
      case MotionEvent.ACTION_UP:
        activeThumbDrawable.setState(new int[0]);

        // Notify OnValueSetListener
        if (valueSetListener != null) {
          if (activeThumbDrawable == leftThumbDrawable) {
            valueSetListener.onMinValueSet(minValue);
          } else if (activeThumbDrawable == rightThumbDrawable) {
            valueSetListener.onMaxValueSet(maxValue);
          }
        }

        activeThumbDrawable = null;
        invalidate();
        return true;
      default:
        return super.onTouchEvent(event);
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    // Min/max value label
    labelPaint.getTextBounds(minLabelText, 0, minLabelText.length(), minValueLabelBounds);
    labelPaint.getTextBounds(maxLabelText, 0, maxLabelText.length(), maxValueLabelBounds);
    setRectXPosition(minValueLabelBounds, labelTextPadding);
    setRectYPosition(minValueLabelBounds, (getMeasuredHeight() / 2) - (minValueLabelBounds.height() / 2));
    setRectXPosition(maxValueLabelBounds, (getMeasuredWidth() - maxValueLabelBounds.width()) - labelTextPadding);
    setRectYPosition(maxValueLabelBounds, (getMeasuredHeight() / 2) - (maxValueLabelBounds.height() / 2));

    // Track
    trackBounds.left = minValueLabelBounds.right + (thumbSize / 2);
    trackBounds.top = (getMeasuredHeight() / 2) - trackHeight;
    trackBounds.right = maxValueLabelBounds.left - (thumbSize / 2);
    trackBounds.bottom = trackBounds.top + trackHeight;

    // Left thumb
    int x = (int) (trackBounds.left - (thumbSize / 2));
    int y = (int) (trackBounds.centerY() - (thumbSize / 2));
    leftThumbDrawable.setBounds(x, y, x + thumbSize, y + thumbSize);

    // Right thumb
    x = (int) (trackBounds.right - (thumbSize / 2));
    rightThumbDrawable.setBounds(x, y, x + thumbSize, y + thumbSize);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    drawTrack(canvas);
    drawTrackFill(canvas);

    leftThumbDrawable.draw(canvas);
    rightThumbDrawable.draw(canvas);

    // Draw min/max value label
    drawLabel(canvas, minLabelText, minValueLabelBounds);
    drawLabel(canvas, maxLabelText, maxValueLabelBounds);

    // Draw min value text
    Rect leftThumbBounds = leftThumbDrawable.getBounds();
    String minValueText = valueFormatter.formatValue(minValue);
    valuePaint.getTextBounds(minValueText, 0, minValueText.length(), sharedTextBounds);
    setRectXPosition(sharedTextBounds, leftThumbBounds.centerX() - (sharedTextBounds.width() / 2));
    setRectYPosition(sharedTextBounds, leftThumbBounds.bottom + valueTextPadding);
    drawValue(canvas, minValueText, sharedTextBounds);

    // Draw max value text
    Rect rightThumbBounds = rightThumbDrawable.getBounds();
    String maxValueText = valueFormatter.formatValue(maxValue);
    valuePaint.getTextBounds(maxValueText, 0, maxValueText.length(), sharedTextBounds);
    setRectXPosition(sharedTextBounds, rightThumbBounds.centerX() - (sharedTextBounds.width() / 2));
    setRectYPosition(sharedTextBounds, rightThumbBounds.bottom + valueTextPadding);
    drawValue(canvas, maxValueText, sharedTextBounds);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Public API
  //////////////////////////////////////////////////////////////////////////////////////////////////

  public void setTrackColor(@ColorInt int color) {
    trackColor = color;
  }

  public void setTrackFillColor(@ColorInt int color) {
    trackFillColor = color;
  }

  public void setTrackHeight(int heightInPx) {
    this.trackHeight = heightInPx;
  }

  public void setMinLabelText(@StringRes int text) {
    setMinLabelText(getResources().getString(text));
  }

  public void setMinLabelText(@NonNull String text) {
    minLabelText = text;
  }

  public void setMaxLabelText(@StringRes int text) {
    setMaxLabelText(getResources().getString(text));
  }

  public void setMaxLabelText(@NonNull String text) {
    maxLabelText = text;
  }

  public void setValueFormatter(@NonNull ValueFormatter formatter) {
    this.valueFormatter = formatter;
  }

  public void setThumbDrawable(@DrawableRes int drawable) {
    setThumbDrawable(getResources().getDrawable(drawable));
  }

  public void setThumbDrawable(Drawable drawable) {
    if (drawable == null) return;
    leftThumbDrawable = drawable;
    rightThumbDrawable = drawable;
  }

  public void setValueTextPadding(int paddingInPx) {
    valueTextPadding = paddingInPx;
  }

  public void setLabelTextPadding(int paddingInPx) {
    labelTextPadding = paddingInPx;
  }

  public void setOnValueSetListener(@Nullable OnValueSetListener valueSetListener) {
    this.valueSetListener = valueSetListener;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Internal methods
  //////////////////////////////////////////////////////////////////////////////////////////////////

  private void applyAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
    if (attrs != null) {
      TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekbar);

      int count = ta.getIndexCount();
      for (int i = 0; i < count; i++) {
        int attr = ta.getIndex(i);
        if (attr == R.styleable.RangeSeekbar_minLabelText) {
          minLabelText = ta.getString(attr);
        } else if (attr == R.styleable.RangeSeekbar_maxLabelText) {
          maxLabelText = ta.getString(attr);
        } else if (attr == R.styleable.RangeSeekbar_labelTextSize) {
          labelTextSize = ta.getDimensionPixelSize(attr, (int) dpToPx(12));
        } else if (attr == R.styleable.RangeSeekbar_valueTextSize) {
          valueTextSize = ta.getDimensionPixelSize(attr, (int) dpToPx(12));
        } else if (attr == R.styleable.RangeSeekbar_valueTextPadding) {
          valueTextPadding = ta.getDimensionPixelSize(attr, (int) dpToPx(4));
        } else if (attr == R.styleable.RangeSeekbar_labelTextPadding) {
          labelTextPadding = ta.getDimensionPixelSize(attr, (int) dpToPx(4));
        } else if (attr == R.styleable.RangeSeekbar_trackHeight) {
          trackHeight = ta.getDimensionPixelSize(attr, (int) dpToPx(3));
        } else if (attr == R.styleable.RangeSeekbar_trackColor) {
          trackColor = ta.getColor(attr, Color.BLACK);
        } else if (attr == R.styleable.RangeSeekbar_trackFillColor) {
          trackFillColor = ta.getColor(attr, Color.BLACK);
        } else if (attr == R.styleable.RangeSeekbar_thumbDrawable) {
          rightThumbDrawable = ta.getDrawable(attr);
          leftThumbDrawable = ta.getDrawable(attr);
        }
      }

      ta.recycle();
    }

    if (minLabelText == null) {
      minLabelText = "Min";
    }
    if (maxLabelText == null) {
      maxLabelText = "Max";
    }
    if (labelTextSize == 0) {
      labelTextSize = (int) dpToPx(12);
    }
    if (valueTextSize == 0) {
      valueTextSize = (int) dpToPx(12);
    }
    if (valueTextPadding == 0) {
      valueTextPadding = (int) dpToPx(4);
    }
    if (labelTextPadding == 0) {
      labelTextPadding = (int) dpToPx(4);
    }
    if (trackHeight == 0) {
      trackHeight = (int) dpToPx(3);
    }
    if (trackColor == 0) {
      trackColor = Color.BLACK;
    }
    if (trackFillColor == 0) {
      trackFillColor = Color.BLACK;
    }
    if (leftThumbDrawable == null) {
      leftThumbDrawable = getResources().getDrawable(R.drawable.ic_thumb_seekbar);
    }
    if (rightThumbDrawable == null) {
      rightThumbDrawable = getResources().getDrawable(R.drawable.ic_thumb_seekbar);
    }
  }

  private float calculateValue(int x) {
    return x / trackBounds.width();
  }

  private void drawValue(Canvas canvas, String text, Rect bounds) {
    canvas.drawText(text, bounds.left, bounds.bottom, valuePaint);
  }

  private void drawLabel(Canvas canvas, String text, Rect bounds) {
    canvas.drawText(text, bounds.left, bounds.bottom, labelPaint);
  }

  private RectF getBoundsBetween(Rect left, Rect right) {
    return new RectF(left.centerX(), trackBounds.top, right.centerX(), trackBounds.bottom);
  }

  private boolean withinBounds(float x, float y, Rect bounds) {
    int padding = (int) dpToPx(4); // Add padding to augment touch target size
    return (x > bounds.left - padding && x < bounds.right + padding) &&
        (y > bounds.top - padding && y < bounds.bottom + padding);
  }

  private void drawTrackFill(Canvas canvas) {
    sharedPaint.reset();
    sharedPaint.setAntiAlias(true);
    sharedPaint.setColor(trackFillColor);
    RectF fill = getBoundsBetween(leftThumbDrawable.getBounds(), rightThumbDrawable.getBounds());
    canvas.drawRoundRect(fill, trackHeight / 2, trackHeight / 2, sharedPaint);
  }

  private void drawTrack(Canvas canvas) {
    sharedPaint.reset();
    sharedPaint.setAntiAlias(true);
    sharedPaint.setColor(trackColor);
    canvas.drawRoundRect(trackBounds, trackHeight / 2, trackHeight / 2, sharedPaint);
  }

  private void moveDrawableHorizontally(int newX, Drawable drawable) {
    Rect original = drawable.getBounds();
    Rect newBounds = new Rect(original);
    setRectXPosition(newBounds, newX);
    setDrawableBounds(drawable, newBounds);
  }

  private void setDrawableBounds(Drawable drawable, Rect newBounds) {
    // Ensure thumbs do not leave the bounds of the track
    if (newBounds.centerX() > trackBounds.right) {
      setRectXPosition(newBounds, (int) trackBounds.right - (newBounds.width() / 2));
    }
    if (newBounds.centerX() < trackBounds.left) {
      setRectXPosition(newBounds, (int) trackBounds.left - (newBounds.width() / 2));
    }

    drawable.setBounds(newBounds);
    invalidate();
  }

  private float dpToPx(float dp) {
    DisplayMetrics dm = getResources().getDisplayMetrics();
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
  }

  /**
   * Sets the new x position on the given Rect, adjusting the left/right bounds.
   */
  private void setRectXPosition(Rect rect, int x) {
    int width = rect.width();
    rect.right = (rect.left = x) + width;
  }

  /**
   * Sets the new y position on the given Rect, adjusting the top/bottom bounds.
   */
  private void setRectYPosition(Rect rect, int y) {
    int height = rect.height();
    rect.bottom = (rect.top = y) + height;
  }

  public interface ValueFormatter {
    String formatValue(float value);
  }

  public interface OnValueSetListener {
    void onMinValueSet(float value);
    void onMaxValueSet(float value);
  }
}
