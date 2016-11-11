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
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import static com.everseat.rangeseekbar.Util.dpToPx;

/**
 * An abstract seek bar
 */
public abstract class AbsSeekbar extends View {
  private Drawable thumbDrawable;

  // Dimensions
  private int trackHeight = 0;
  private int labelTextPadding = 0;
  private int labelTextSize = 0;
  private int valueTextPadding = 0;

  // Size holders
  Rect sharedTextBounds = new Rect();
  private RectF trackBounds = new RectF();
  private Rect minLabelBounds = new Rect();
  private Rect maxLabelBounds = new Rect();

  // Paint
  private Paint valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
  private Paint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
  private Paint sharedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  // State values
  private int trackColor = 0;
  private int trackFillColor = 0;
  private float progress = 0f;
  private String minLabelText;
  private String maxLabelText;
  static final int[] STATE_PRESSED = new int[] {android.R.attr.state_pressed};
  static final int[] STATE_DEFAULT = new int[] {};

  private ValueFormatter valueFormatter = new ValueFormatter() {
    @Override
    public String formatValue(float value) {
      return String.valueOf(value);
    }
  };

  public AbsSeekbar(Context context) {
    super(context);
    init(context, null);
  }

  public AbsSeekbar(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public AbsSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    applyAttributes(context, attrs);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    // Track
    trackBounds.left = minLabelBounds.right + labelTextPadding + getTrackLeftOffset();
    trackBounds.top = (getMeasuredHeight() / 2) - trackHeight;
    trackBounds.right = maxLabelBounds.left - labelTextPadding - getTrackRightOffset();
    trackBounds.bottom = trackBounds.top + trackHeight;

    // Min/max value label
    labelPaint.getTextBounds(minLabelText, 0, minLabelText.length(), minLabelBounds);
    labelPaint.getTextBounds(maxLabelText, 0, maxLabelText.length(), maxLabelBounds);
    setRectXPosition(minLabelBounds, labelTextPadding);
    setRectYPosition(minLabelBounds, (int) trackBounds.centerY() - (minLabelBounds.height() / 2));
    setRectXPosition(maxLabelBounds, (getMeasuredWidth() - maxLabelBounds.width()) - labelTextPadding);
    setRectYPosition(maxLabelBounds, (int) (trackBounds.centerY() - (minLabelBounds.height() / 2)));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    drawTrack(canvas);
    onDrawTrackDecoration(canvas, sharedPaint);

    // Draw min/max value label
    drawLabel(canvas, minLabelText, minLabelBounds);
    drawLabel(canvas, maxLabelText, maxLabelBounds);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    setAlpha(enabled ? 1f : 0.5f);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    if (!isEnabled()) return false;
    return super.dispatchTouchEvent(event);
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

  private void setLabelTextSize(float textSize) {
    labelPaint.setTextSize(textSize);
  }

  public void setValueFormatter(@NonNull ValueFormatter formatter) {
    this.valueFormatter = formatter;
  }

  public void setLabelTextPadding(int paddingInPx) {
    labelTextPadding = paddingInPx;
  }

  public float getProgress() {
    return progress;
  }

  public void setProgress(float progress) {
    this.progress = progress;
    invalidate();
  }

  public int getTrackFillColor() {
    return trackFillColor;
  }

  public int getTrackHeight() {
    return trackHeight;
  }

  public RectF getTrackBounds() {
    return trackBounds;
  }

  public void setValueTextPadding(int paddingInPx) {
    valueTextPadding = paddingInPx;
  }

  public void setValueTextSize(float textSize) {
    valuePaint.setTextSize(textSize);
  }

  public int getValueTextPadding() {
    return valueTextPadding;
  }

  public void setThumbDrawable(@DrawableRes int drawable) {
    setThumbDrawable(getResources().getDrawable(drawable));
  }

  public void setThumbDrawable(Drawable drawable) {
    thumbDrawable = drawable;
  }

  public Drawable getThumbDrawable() {
    return thumbDrawable;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Internal methods
  //////////////////////////////////////////////////////////////////////////////////////////////////

  private void applyAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
    String minLabelText = null;
    String maxLabelText = null;
    float labelTextSize = 0;
    int labelTextPadding = 0;
    int trackHeight = 0;
    int trackColor = 0;
    int trackFillColor = 0;
    float valueTextSize = 0;
    int valueTextPadding = 0;
    Drawable thumbDrawable = null;

    if (attrs != null) {
      TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AbsSeekbar);
      int count = ta.getIndexCount();
      for (int i = 0; i < count; i++) {
        int attr = ta.getIndex(i);
        if (attr == R.styleable.AbsSeekbar_minLabelText) {
          minLabelText = ta.getString(attr);
        } else if (attr == R.styleable.AbsSeekbar_maxLabelText) {
          maxLabelText = ta.getString(attr);
        } else if (attr == R.styleable.AbsSeekbar_labelTextSize) {
          labelTextSize = ta.getDimension(attr, dpToPx(getResources(), 12));
        } else if (attr == R.styleable.AbsSeekbar_labelTextPadding) {
          labelTextPadding = ta.getDimensionPixelSize(attr, (int) dpToPx(getResources(), 4));
        } else if (attr == R.styleable.AbsSeekbar_trackHeight) {
          trackHeight = ta.getDimensionPixelSize(attr, (int) dpToPx(getResources(), 3));
        } else if (attr == R.styleable.AbsSeekbar_trackColor) {
          trackColor = ta.getColor(attr, Color.BLACK);
        } else if (attr == R.styleable.AbsSeekbar_trackFillColor) {
          trackFillColor = ta.getColor(attr, Color.BLACK);
        } else if (attr == R.styleable.AbsSeekbar_valueTextSize) {
          valueTextSize = ta.getDimension(attr, dpToPx(getResources(), 14));
        } else if (attr == R.styleable.AbsSeekbar_valueTextPadding) {
          valueTextPadding = ta.getDimensionPixelSize(attr, (int) dpToPx(getResources(), 4));
        } else if (attr == R.styleable.AbsSeekbar_thumbDrawable) {
          thumbDrawable = ta.getDrawable(attr);
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
      labelTextSize = dpToPx(getResources(), 12);
    }
    if (labelTextPadding == 0) {
      labelTextPadding = (int) dpToPx(getResources(), 4);
    }
    if (trackHeight == 0) {
      trackHeight = (int) dpToPx(getResources(), 3);
    }
    if (trackColor == 0) {
      trackColor = Color.BLACK;
    }
    if (trackFillColor == 0) {
      trackFillColor = Color.BLACK;
    }
    if (valueTextSize == 0) {
      valueTextSize = dpToPx(getResources(), 14);
    }
    if (valueTextPadding == 0) {
      valueTextPadding = (int) dpToPx(getResources(), 4);
    }
    if (thumbDrawable == null) {
      thumbDrawable = getResources().getDrawable(R.drawable.ic_thumb_seekbar);
    }

    setMinLabelText(minLabelText);
    setMaxLabelText(maxLabelText);
    setLabelTextSize(labelTextSize);
    setLabelTextPadding(labelTextPadding);
    setTrackHeight(trackHeight);
    setTrackColor(trackColor);
    setTrackFillColor(trackFillColor);
    setValueTextSize(valueTextSize);
    setValueTextPadding(valueTextPadding);
    setThumbDrawable(thumbDrawable);
  }

  private void drawLabel(Canvas canvas, String text, Rect bounds) {
    canvas.drawText(text, bounds.left, bounds.bottom, labelPaint);
  }

  private void drawTrack(Canvas canvas) {
    sharedPaint.reset();
    sharedPaint.setAntiAlias(true);
    sharedPaint.setColor(trackColor);
    canvas.drawRoundRect(trackBounds, trackHeight / 2, trackHeight / 2, sharedPaint);
  }

  protected String formatValue(float value) {
    return valueFormatter.formatValue(value);
  }

  /**
   * Sets the new x position on the given Rect, adjusting the left/right bounds.
   */
  protected void setRectXPosition(Rect rect, int x) {
    int width = rect.width();
    rect.right = (rect.left = x) + width;
  }

  /**
   * Sets the new y position on the given Rect, adjusting the top/bottom bounds.
   */
  protected void setRectYPosition(Rect rect, int y) {
    int height = rect.height();
    rect.bottom = (rect.top = y) + height;
  }

  protected void setRectCenterX(Rect rect, int newCenterX) {
    int width = rect.width();
    int currentCenterX = rect.left + width/2;
    int diff = newCenterX - currentCenterX;
    setRectXPosition(rect, rect.left + diff);
  }

  protected void setRectCenterY(Rect rect, int newCenterY) {
    int height = rect.height();
    int currentCenterY = rect.top + height/2;
    int diff = newCenterY - currentCenterY;
    setRectYPosition(rect, rect.top + diff);
  }

  protected void onDrawTrackDecoration(Canvas canvas, Paint sharedPaint) {
    sharedPaint.reset();
    sharedPaint.setAntiAlias(true);
    sharedPaint.setColor(trackFillColor);
    RectF fill = new RectF(0, trackBounds.top, getProgress(), trackBounds.bottom);
    canvas.drawRoundRect(fill, trackHeight / 2, trackHeight / 2, sharedPaint);
  }

  void measureText(String text, Rect outRect) {
    valuePaint.getTextBounds(text, 0, text.length(), outRect);
  }

  float calculateValue(int x) {
    if (x < 0) return 0;
    if (x > trackBounds.width()) return 1;
    return x / trackBounds.width();
  }

  void drawValueText(Canvas canvas, String text, Rect bounds) {
    canvas.drawText(text, bounds.left, bounds.bottom, valuePaint);
  }

  private int getTrackLeftOffset() {
    return getThumbSize();
  }

  private int getTrackRightOffset() {
    return getThumbSize();
  }

  private int getThumbSize() {
    if (thumbDrawable == null) return 0;
    return Math.max(thumbDrawable.getIntrinsicWidth(), thumbDrawable.getIntrinsicHeight());
  }

  public interface ValueFormatter {
    String formatValue(float value);
  }
}
