/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codetroopers.betterpickers.calendardatepicker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.TextView;

import com.codetroopers.betterpickers.R;

/**
 * A text view which, when pressed or activated, displays a blue circle around the text.
 */
public class TextViewWithCircularIndicator extends TextView {

    private static final String TAG = TextViewWithCircularIndicator.class.getSimpleName();

    public static final int SELECTED_CIRCLE_ALPHA = 60;
    private static final int ALPHA_MAX_VALUE = 255;

    Paint mCirclePaint = new Paint();

    private final int mRadius;
    private int mCircleColor;
    private final String mItemIsSelectedText;
    private int mSelectedTextColor;

    private int mSelectionAlpha;

    private boolean mDrawCircle;

    public TextViewWithCircularIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        mCircleColor = ContextCompat.getColor(context, R.color.bpBlue);
        mRadius = res.getDimensionPixelOffset(R.dimen.month_select_circle_radius);
        mItemIsSelectedText = context.getResources().getString(R.string.item_is_selected);

        init();
    }

    private void init() {
        mCirclePaint.setFakeBoldText(true);
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setColor(mCircleColor);
        mCirclePaint.setTextAlign(Align.CENTER);
        mCirclePaint.setStyle(Style.FILL);
        mCirclePaint.setAlpha(mSelectionAlpha);
    }

    public void setCircleColor(int circleColor) {
        this.mCircleColor = circleColor;
        this.init();
    }

    public void drawIndicator(boolean drawCircle) {
        mDrawCircle = drawCircle;
    }

    public void setCircleAlpha(int alpha) {
        if (alpha >= 0 && alpha <= ALPHA_MAX_VALUE) {
            mSelectionAlpha = alpha;
        }
    }

    public void setSelectedTextColor(@ColorInt int selectedTextColor) {
        mSelectedTextColor = selectedTextColor;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mDrawCircle) {
            final int width = getWidth();
            final int height = getHeight();
            int radius = Math.min(width, height) / 2;
            canvas.drawCircle(width / 2, height / 2, radius, mCirclePaint);
            setTextColor(mSelectedTextColor);
        }
        super.onDraw(canvas);
    }

    @Override
    public CharSequence getContentDescription() {
        CharSequence itemText = getText();
        if (mDrawCircle) {
            return String.format(mItemIsSelectedText, itemText);
        } else {
            return itemText;
        }
    }
}
