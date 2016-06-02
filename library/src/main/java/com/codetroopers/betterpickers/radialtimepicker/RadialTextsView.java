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

package com.codetroopers.betterpickers.radialtimepicker;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.codetroopers.betterpickers.R;
import com.nineoldandroids.animation.Keyframe;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.animation.AnimatorProxy;

/**
 * A view to show a series of numbers in a circular pattern.
 */
public class RadialTextsView extends View {

    private final static String TAG = "RadialTextsView";

    private final Paint mPaint = new Paint();

    private boolean mDrawValuesReady;
    private boolean mIsInitialized;

    private Typeface mTypefaceLight;
    private Typeface mTypefaceRegular;
    private String[] mTexts;
    private String[] mInnerTexts;
    private boolean mIs24HourMode;
    private boolean mHasInnerCircle;
    private float mCircleRadiusMultiplier;
    private float mAmPmCircleRadiusMultiplier;
    private float mNumbersRadiusMultiplier;
    private float mInnerNumbersRadiusMultiplier;
    private float mTextSizeMultiplier;
    private float mInnerTextSizeMultiplier;

    private int mXCenter;
    private int mYCenter;
    private float mCircleRadius;
    private boolean mTextGridValuesDirty;
    private float mTextSize;
    private float mInnerTextSize;
    private float[] mTextGridHeights;
    private float[] mTextGridWidths;
    private float[] mInnerTextGridHeights;
    private float[] mInnerTextGridWidths;

    private float mAnimationRadiusMultiplier;
    private float mTransitionMidRadiusMultiplier;
    private float mTransitionEndRadiusMultiplier;
    ObjectAnimator mDisappearAnimator;
    ObjectAnimator mReappearAnimator;
    private InvalidateUpdateListener mInvalidateUpdateListener;

    private int mSelectedTextColor;
    private int mNumbersTextColor;
    private Path mSelectorPath;

    public RadialTextsView(Context context) {
        super(context);
        mIsInitialized = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    public void initialize(Resources res, String[] texts, String[] innerTexts,
            boolean is24HourMode, boolean disappearsOut) {
        if (mIsInitialized) {
            Log.e(TAG, "This RadialTextsView may only be initialized once.");
            return;
        }

        // Set up the paint.
        int numbersTextColor = res.getColor(R.color.numbers_text_color);
        mPaint.setColor(numbersTextColor);
        String typefaceFamily = res.getString(R.string.radial_numbers_typeface);
        mTypefaceLight = Typeface.create(typefaceFamily, Typeface.NORMAL);
        String typefaceFamilyRegular = res.getString(R.string.sans_serif);
        mTypefaceRegular = Typeface.create(typefaceFamilyRegular, Typeface.NORMAL);
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Align.CENTER);

        mTexts = texts;
        mInnerTexts = innerTexts;
        mIs24HourMode = is24HourMode;
        mHasInnerCircle = (innerTexts != null);

        // Calculate the radius for the main circle.
        if (is24HourMode) {
            mCircleRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.circle_radius_multiplier_24HourMode));
        } else {
            mCircleRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.circle_radius_multiplier));
            mAmPmCircleRadiusMultiplier =
                    Float.parseFloat(res.getString(R.string.ampm_circle_radius_multiplier));
        }

        // Initialize the widths and heights of the grid, and calculate the values for the numbers.
        mTextGridHeights = new float[12];
        mTextGridWidths = new float[12];
        if (mHasInnerCircle) {
            mNumbersRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.numbers_radius_multiplier_outer));
            mTextSizeMultiplier = Float.parseFloat(
                    res.getString(R.string.text_size_multiplier_outer));
            mInnerNumbersRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.numbers_radius_multiplier_inner));
            mInnerTextSizeMultiplier = Float.parseFloat(
                    res.getString(R.string.text_size_multiplier_inner));

            mInnerTextGridHeights = new float[12];
            mInnerTextGridWidths = new float[12];
        } else {
            mNumbersRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.numbers_radius_multiplier_normal));
            mTextSizeMultiplier = Float.parseFloat(
                    res.getString(R.string.text_size_multiplier_normal));
        }

        mAnimationRadiusMultiplier = 1;
        mTransitionMidRadiusMultiplier = 1f + (0.05f * (disappearsOut ? -1 : 1));
        mTransitionEndRadiusMultiplier = 1f + (0.3f * (disappearsOut ? 1 : -1));
        mInvalidateUpdateListener = new InvalidateUpdateListener();

        mTextGridValuesDirty = true;
        mIsInitialized = true;
    }

    /* package */ void setTheme(TypedArray themeColors) {
        mNumbersTextColor = themeColors.getColor(R.styleable.BetterPickersDialog_bpMainTextColor, ContextCompat.getColor(getContext(), R.color.numbers_text_color));
        mSelectedTextColor = themeColors.getColor(R.styleable.BetterPickersDialog_bpContrastTextColor, ContextCompat.getColor(getContext(), R.color.bpWhite));
    }

    /**
     * Allows for smoother animation.
     */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    /**
     * Used by the animation to move the numbers in and out.
     */
    public void setAnimationRadiusMultiplier(float animationRadiusMultiplier) {
        mAnimationRadiusMultiplier = animationRadiusMultiplier;
        mTextGridValuesDirty = true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int viewWidth = getWidth();
        if (viewWidth == 0 || !mIsInitialized) {
            return;
        }

        if (!mDrawValuesReady) {
            mXCenter = getWidth() / 2;
            mYCenter = getHeight() / 2;
            mCircleRadius = Math.min(mXCenter, mYCenter) * mCircleRadiusMultiplier;
            if (!mIs24HourMode) {
                // We'll need to draw the AM/PM circles, so the main circle will need to have
                // a slightly higher center. To keep the entire view centered vertically, we'll
                // have to push it up by half the radius of the AM/PM circles.
                float amPmCircleRadius = mCircleRadius * mAmPmCircleRadiusMultiplier;
                mYCenter -= amPmCircleRadius / 2;
            }

            mTextSize = mCircleRadius * mTextSizeMultiplier;
            if (mHasInnerCircle) {
                mInnerTextSize = mCircleRadius * mInnerTextSizeMultiplier;
            }

            // Because the text positions will be static, pre-render the animations.
            renderAnimations();

            mTextGridValuesDirty = true;
            mDrawValuesReady = true;
        }

        // Calculate the text positions, but only if they've changed since the last onDraw.
        if (mTextGridValuesDirty) {
            float numbersRadius =
                    mCircleRadius * mNumbersRadiusMultiplier * mAnimationRadiusMultiplier;

            // Calculate the positions for the 12 numbers in the main circle.
            calculateGridSizes(numbersRadius, mXCenter, mYCenter,
                    mTextSize, mTextGridHeights, mTextGridWidths);
            if (mHasInnerCircle) {
                // If we have an inner circle, calculate those positions too.
                float innerNumbersRadius =
                        mCircleRadius * mInnerNumbersRadiusMultiplier * mAnimationRadiusMultiplier;
                calculateGridSizes(innerNumbersRadius, mXCenter, mYCenter,
                        mInnerTextSize, mInnerTextGridHeights, mInnerTextGridWidths);
            }
            mTextGridValuesDirty = false;
        }

        // Draw the texts in the pre-calculated positions.
        drawTexts(canvas, mTextSize, mTypefaceLight, mTexts, mTextGridWidths,
                mTextGridHeights, true);
        if (mHasInnerCircle) {
            drawTexts(canvas, mInnerTextSize, mTypefaceRegular, mInnerTexts, mInnerTextGridWidths,
                    mInnerTextGridHeights, true);
        }
    }

    /**
     * Using the trigonometric Unit Circle, calculate the positions that the text will need to be drawn at based on the
     * specified circle radius. Place the values in the textGridHeights and textGridWidths parameters.
     */
    private void calculateGridSizes(float numbersRadius, float xCenter, float yCenter,
            float textSize, float[] textGridHeights, float[] textGridWidths) {
        /*
         * The numbers need to be drawn in a 7x7 grid, representing the points on the Unit Circle.
         * Each numbers co-ordinates are caluclated separately even when they are the same to allow
         * easier iterating through the values.
         */
        float offset1 = numbersRadius;
        // cos(30) = a / r => r * cos(30) = a => r * √3/2 = a
        float offset2 = numbersRadius * ((float) Math.sqrt(3)) / 2f;
        // sin(30) = o / r => r * sin(30) = o => r / 2 = a
        float offset3 = numbersRadius / 2f;
        mPaint.setTextSize(textSize);
        // We'll need yTextBase to be slightly lower to account for the text's baseline.
        yCenter -= (mPaint.descent() + mPaint.ascent()) / 2;

        textGridHeights[0] = yCenter - offset1;
        textGridHeights[1] = yCenter - offset2;
        textGridHeights[2] = yCenter - offset3;
        textGridHeights[3] = yCenter;
        textGridHeights[4] = yCenter + offset3;
        textGridHeights[5] = yCenter + offset2;
        textGridHeights[6] = yCenter + offset1;
        textGridHeights[7] = yCenter + offset2;
        textGridHeights[8] = yCenter + offset3;
        textGridHeights[9] = yCenter;
        textGridHeights[10] = yCenter - offset3;
        textGridHeights[11] = yCenter - offset2;

        textGridWidths[0] = xCenter;
        textGridWidths[1] = xCenter + offset3;
        textGridWidths[2] = xCenter + offset2;
        textGridWidths[3] = xCenter + offset1;
        textGridWidths[4] = xCenter + offset2;
        textGridWidths[5] = xCenter + offset3;
        textGridWidths[6] = xCenter;
        textGridWidths[7] = xCenter - offset3;
        textGridWidths[8] = xCenter - offset2;
        textGridWidths[9] = xCenter - offset1;
        textGridWidths[10] = xCenter - offset2;
        textGridWidths[11] = xCenter - offset3;
    }

    /**
     * Draw the 12 text values at the positions specified by the textGrid parameters.
     *
     * If showing selection contrast, initially draw all numbers outside of the current value of mSelectorPath,
     * and then recursively call drawTexts() again and draw numbers inside mSelectorPath
     */
    public void drawTexts(Canvas canvas, float textSize, Typeface typeface, String[] texts, float[] textGridWidths,
                          float[] textGridHeights, boolean showSelectionContrast) {

        mPaint.setTextSize(textSize);
        mPaint.setTypeface(typeface);

        canvas.save();

        if (mSelectorPath != null) {
            if (showSelectionContrast) {
                mPaint.setColor(mSelectedTextColor);
                canvas.clipPath(mSelectorPath, Region.Op.REPLACE);
            } else {
                mPaint.setColor(mNumbersTextColor);
                canvas.clipPath(mSelectorPath, Region.Op.XOR);
            }
        } else {
            mPaint.setColor(mNumbersTextColor);
        }

        for (int i = 0; i < texts.length; i++) {

            if (texts.length == textGridHeights.length && textGridHeights.length == textGridWidths.length) {
                canvas.drawText(texts[i], textGridWidths[i], textGridHeights[i], mPaint);
            }
        }

        canvas.restore();

        if (showSelectionContrast) {
            drawTexts(canvas, textSize, typeface, texts, textGridWidths, textGridHeights, false);
        }
    }

    /**
     * Render the animations for appearing and disappearing.
     */
    private void renderAnimations() {
        Keyframe kf0, kf1, kf2, kf3;
        float midwayPoint = 0.2f;
        int duration = 500;

        // Set up animator for disappearing.
        kf0 = Keyframe.ofFloat(0f, 1);
        kf1 = Keyframe.ofFloat(midwayPoint, mTransitionMidRadiusMultiplier);
        kf2 = Keyframe.ofFloat(1f, mTransitionEndRadiusMultiplier);
        PropertyValuesHolder radiusDisappear = PropertyValuesHolder.ofKeyframe(
                "animationRadiusMultiplier", kf0, kf1, kf2);

        kf0 = Keyframe.ofFloat(0f, 1f);
        kf1 = Keyframe.ofFloat(1f, 0f);
        PropertyValuesHolder fadeOut = PropertyValuesHolder.ofKeyframe("alpha", kf0, kf1);

        mDisappearAnimator = ObjectAnimator.ofPropertyValuesHolder(
                AnimatorProxy.NEEDS_PROXY ? AnimatorProxy.wrap(this) : this, radiusDisappear, fadeOut)
                .setDuration(duration);
        mDisappearAnimator.addUpdateListener(mInvalidateUpdateListener);

        // Set up animator for reappearing.
        float delayMultiplier = 0.25f;
        float transitionDurationMultiplier = 1f;
        float totalDurationMultiplier = transitionDurationMultiplier + delayMultiplier;
        int totalDuration = (int) (duration * totalDurationMultiplier);
        float delayPoint = (delayMultiplier * duration) / totalDuration;
        midwayPoint = 1 - (midwayPoint * (1 - delayPoint));

        kf0 = Keyframe.ofFloat(0f, mTransitionEndRadiusMultiplier);
        kf1 = Keyframe.ofFloat(delayPoint, mTransitionEndRadiusMultiplier);
        kf2 = Keyframe.ofFloat(midwayPoint, mTransitionMidRadiusMultiplier);
        kf3 = Keyframe.ofFloat(1f, 1);
        PropertyValuesHolder radiusReappear = PropertyValuesHolder.ofKeyframe(
                "animationRadiusMultiplier", kf0, kf1, kf2, kf3);

        kf0 = Keyframe.ofFloat(0f, 0f);
        kf1 = Keyframe.ofFloat(delayPoint, 0f);
        kf2 = Keyframe.ofFloat(1f, 1f);
        PropertyValuesHolder fadeIn = PropertyValuesHolder.ofKeyframe("alpha", kf0, kf1, kf2);

        mReappearAnimator = ObjectAnimator.ofPropertyValuesHolder(
                AnimatorProxy.NEEDS_PROXY ? AnimatorProxy.wrap(this) : this, radiusReappear, fadeIn)
                .setDuration(totalDuration);
        mReappearAnimator.addUpdateListener(mInvalidateUpdateListener);
    }

    /**
     * Set the Path value currently occupied by the selector object, and redraw the view
     * @param selectorPath the Path to set
     */
    public void setSelection(Path selectorPath) {

            mSelectorPath = selectorPath;
            invalidate();

    }

    public ObjectAnimator getDisappearAnimator() {
        if (!mIsInitialized || !mDrawValuesReady || mDisappearAnimator == null) {
            Log.e(TAG, "RadialTextView was not ready for animation.");
            return null;
        }

        return mDisappearAnimator;
    }

    public ObjectAnimator getReappearAnimator() {
        if (!mIsInitialized || !mDrawValuesReady || mReappearAnimator == null) {
            Log.e(TAG, "RadialTextView was not ready for animation.");
            return null;
        }

        return mReappearAnimator;
    }

    private class InvalidateUpdateListener implements ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            RadialTextsView.this.invalidate();
        }
    }
}
