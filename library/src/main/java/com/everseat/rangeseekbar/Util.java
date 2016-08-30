package com.everseat.rangeseekbar;

import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class Util {
  static float dpToPx(Resources resources, float dp) {
    DisplayMetrics dm = resources.getDisplayMetrics();
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
  }

  static boolean withinBounds(float x, float y, Rect bounds) {
    return (x > bounds.left && x < bounds.right) &&
        (y > bounds.top && y < bounds.bottom);
  }

  static Rect expandRect(Rect rect, int value) {
    return new Rect(rect.left - value, rect.top - value, rect.right + value, rect.bottom + value);
  }
}
