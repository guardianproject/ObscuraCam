package org.witness.obscuracam.ui;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

// This behavior fixes the problem that the view is not pinned to the bottom of the coordinator layout
// Fix for this: https://code.google.com/p/android/issues/detail?id=177195
//
public class ScrollViewBehaviorFix extends AppBarLayout.ScrollingViewBehavior {

    private AppBarLayout appBarLayout;

    public ScrollViewBehaviorFix() {
        super();
    }

    public ScrollViewBehaviorFix(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {

        if (appBarLayout == null) {
            appBarLayout = (AppBarLayout) dependency;
        }

        final boolean result = super.onDependentViewChanged(parent, child, dependency);

        int height = child.getHeight();
        int newHeight = parent.getHeight() - dependency.getBottom();
        int bottomPadding = height - newHeight;

        //final int bottomPadding = calculateBottomPadding(appBarLayout);
        final boolean paddingChanged = bottomPadding != child.getPaddingBottom();
        if (paddingChanged) {
            child.setPadding(
                    child.getPaddingLeft(),
                    child.getPaddingTop(),
                    child.getPaddingRight(),
                    bottomPadding);
            child.requestLayout();
        }
        return paddingChanged || result;
    }
}