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
import android.graphics.Canvas;
import android.graphics.Typeface;

import com.codetroopers.betterpickers.Utils;

/**
 * A calendar-like view displaying a specified month and the appropriate selectable day numbers within the specified
 * month.
 */
public class SimpleMonthView extends MonthView {

    public SimpleMonthView(Context context) {
        super(context);
    }

    public SimpleMonthView(Context context, Typeface regularTypeface, Typeface boldTypeface) {
        super(context, regularTypeface, boldTypeface);
    }

    @Override
    public void drawMonthDay(Canvas canvas, int year, int month, int day,
            int x, int y, int startX, int stopX, int startY, int stopY, boolean isEnabled) {

        boolean isSelected = (mSelectedDay == day);

        if (isSelected) {
            canvas.drawCircle(x, y - (MINI_DAY_NUMBER_TEXT_SIZE / 3), DAY_SELECTED_CIRCLE_SIZE,
                    mSelectedCirclePaint);
        }
        int disabledDayKey = Utils.formatDisabledDayForKey(year, month, day);
        // If this day is disabled, color the background
        if (mDisabledDays != null
                && mDisabledDays.indexOfKey(disabledDayKey) > 0) {
            canvas.drawRect(startX, startY, stopX, stopY, mDisabledDaySquarePaint);
        }

        if (isSelected) {
            mMonthNumPaint.setColor(mSelectedTextColor);
        } else if (mHighlightToday && mHasToday && mToday == day) {
            mMonthNumPaint.setColor(mTodayNumberColor);
        } else if (isEnabled) {
            mMonthNumPaint.setColor(mDayTextColorEnabled);
        } else {
            mMonthNumPaint.setColor(mDayTextColorDisabled);
        }
        canvas.drawText(String.format("%d", day), x, y, mMonthNumPaint);
    }
}
