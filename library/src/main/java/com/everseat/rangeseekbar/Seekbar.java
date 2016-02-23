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
 * A simple seek bar
 */
public class Seekbar extends AbsSeekbar {
  private Drawable thumbDrawable;

  // Dimensions
  private int valueTextPadding = 0;
  private int thumbSize = 0;

  // Size holders
  private Rect sharedTextBounds = new Rect();
  private RectF trackBounds = new RectF();
  private Rect thumbBounds = new Rect();

  // Paint
  private Paint valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

  // State values
  private float currentValue = 0f;

  @Nullable private OnValueSetListener valueSetListener;

  protected static final int[] STATE_PRESSED = new int[] {android.R.attr.state_pressed};
  protected static final int[] STATE_DEFAULT = new int[] {};

  public Seekbar(Context context) {
    super(context);
    init(context, null);
  }

  public Seekbar(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public Seekbar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    int valueTextSize = 0;
    int valueTextPadding = 0;
    Drawable thumbDrawable = null;

    if (attrs != null) {
      TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.Seekbar);

      int count = ta.getIndexCount();
      for (int i = 0; i < count; i++) {
        int attr = ta.getIndex(i);
        if (attr == R.styleable.Seekbar_valueTextSize) {
          valueTextSize = ta.getInt(attr, (int) dpToPx(14));
        } else if (attr == R.styleable.Seekbar_valueTextPadding) {
          valueTextPadding = ta.getInt(attr, (int) dpToPx(4));
        } else if (attr == R.styleable.Seekbar_thumbDrawable) {
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
        if (withinBounds(x, y, thumbBounds)) {
          if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
          }

          thumbDrawable.setState(STATE_PRESSED);
          invalidate();
          return true;
        }

        return false;
      case MotionEvent.ACTION_MOVE:
        // We only support a horizontal bar at the moment
        int radius = thumbBounds.width() / 2;
        float newX = (int) event.getX() - radius;

        setRectXPosition(thumbBounds, (int) newX);

        if (thumbBounds.centerX() >= trackBounds.right) {
          setRectXPosition(thumbBounds, (int) (trackBounds.right - (thumbBounds.width()/2)));
        }
        if (thumbBounds.centerX() <= trackBounds.left) {
          setRectXPosition(thumbBounds, (int) (trackBounds.left - (thumbBounds.width()/2)));
        }

        // We want the center of the thumb drawable to be the deciding factor
        currentValue = calculateValue((int) (newX + radius - trackBounds.left));

        invalidate();
        return true;
      case MotionEvent.ACTION_UP:
        // Notify OnValueSetListener
        if (valueSetListener != null) {
          valueSetListener.onValueSet(currentValue);
        }

        thumbDrawable.setState(STATE_DEFAULT);
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

    float offset = currentValue * trackBounds.width();
    int x = (int) (trackBounds.left + offset) - (thumbSize / 2);
    int y = (int) (trackBounds.centerY() - (thumbSize / 2));
    thumbBounds.set(x, y, x + thumbSize, y + thumbSize);
    thumbDrawable.setBounds(thumbBounds);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    onDrawThumb(canvas);

    // Draw current value text
    String text = formatValue(currentValue);
    Rect textBounds = measureValueText(text);
    setRectXPosition(textBounds, thumbBounds.centerX() - (textBounds.width() / 2));
    setRectYPosition(textBounds, thumbBounds.bottom + valueTextPadding);
    onDrawValueText(canvas, textBounds, text);
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

  public void setCurrentValue(float value) {
    currentValue = value;
  }

  public Drawable getThumbDrawable() {
    return thumbDrawable;
  }

  public float getCurrentValue() {
    return currentValue;
  }

  public int getValueTextPadding() {
    return valueTextPadding;
  }

  public Rect getThumbBounds() {
    return thumbBounds;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Internal methods
  //////////////////////////////////////////////////////////////////////////////////////////////////

  private void drawValue(Canvas canvas, String text, Rect bounds) {
    canvas.drawText(text, bounds.left, bounds.bottom, valuePaint);
  }

  protected boolean withinBounds(float x, float y, Rect bounds) {
    int padding = (int) dpToPx(4); // Add padding to augment touch target size
    return (x > bounds.left - padding && x < bounds.right + padding) &&
        (y > bounds.top - padding && y < bounds.bottom + padding);
  }

  protected float calculateValue(int x) {
    if (x < 0) return 0;
    if (x > trackBounds.width()) return 1;
    return x / trackBounds.width();
  }

  protected void onDrawThumb(Canvas canvas) {
    thumbDrawable.setBounds(thumbBounds);
    thumbDrawable.draw(canvas);
  }

  @Override
  protected void onDrawTrackDecoration(Canvas canvas, Paint sharedPaint) {
    sharedPaint.reset();
    sharedPaint.setAntiAlias(true);
    sharedPaint.setColor(getTrackFillColor());
    RectF fill = new RectF(trackBounds.left,
        trackBounds.top,
        thumbBounds.centerX(),
        trackBounds.bottom);
    canvas.drawRoundRect(fill, getTrackHeight() / 2, getTrackHeight() / 2, sharedPaint);
  }

  protected void onDrawValueText(Canvas canvas, Rect textBounds, String text) {
    drawValue(canvas, text, textBounds);
  }

  @Override
  protected int getTrackLeftOffset() {
    return thumbBounds.width() / 2;
  }

  @Override
  protected int getTrackRightOffset() {
    return thumbBounds.width() / 2;
  }

  protected Rect measureValueText(String text) {
    valuePaint.getTextBounds(text, 0, text.length(), sharedTextBounds);
    return sharedTextBounds;
  }

  public interface OnValueSetListener {
    void onValueSet(float value);
  }
}
