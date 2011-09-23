package org.witness.securesmartcam.filters;

import android.graphics.Canvas;
import android.graphics.Rect;

public interface ObscureMethod {
	void obscureRect(Rect rect, Canvas canvas);
}
