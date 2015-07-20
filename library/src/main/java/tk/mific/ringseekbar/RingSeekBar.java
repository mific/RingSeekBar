package tk.mific.ringseekbar;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import tk.mific.ringseekbar.R;

public class RingSeekBar extends FrameLayout {

    private final static String TAG = "CustomRingProgress";

    private final static String PARENT_STATE = "PARENT_STATE";

    private final static String CURRENT_ANGLE = "CURRENT_ANGLE";
    private final static String MAX_ANGLE = "MAX_ANGLE";
    private final static String MIN_ANGLE = "MIN_ANGLE";
    private final static String VALUE_RATIO = "VALUE_RATIO";
    private final static String IS_ACTIVE = "IS_ACTIVE";
    private final static String VELOCITY_RATIO = "VELOCITY_RATIO";
    private final static String FLING_SENSITIVITY = "FLING_SENSITIVITY";
    private final static String FLING_DURATION = "FLING_DURATION";
    private final static String IS_FADE = "IS_FADE";
    private final static String THICKNESS = "THICKNESS";

    // Default values
    private static final int DEFAULT_VALUE = 0;
    private static final int DEFAULT_MAX = 99;
    private static final int DEFAULT_MIN = -99;
    private static final float DEFAULT_VALUE_RATIO = 1;
    private static final int DEFAULT_VELOCITY_RATIO = 25;
    private static final int DEFAULT_FLING_SENSITIVITY = 20;
    private static final int DEFAULT_FLING_DURATION = 1200;
    private static final boolean DEFAULT_ACTIVE = false;
    private static final boolean DEFAULT_FADE_IN = true;
    private static final int DEFAULT_FADE_DURATION = 1000;
    private static final float DEFAULT_FADE_FROM = 0.1f;
    private static final float DEFAULT_FADE_TO = 1.0f;
    private static final int DEFAULT_THICKNESS = 20;


    private final float density = getResources().getDisplayMetrics().density;

    private int mRingPadding = (int)(10 * density);

    // max angle
    private float mMaxAngle;

    // min angle
    private float mMinAngle;

    //dimensions of component
    private int mHeight;
    private int mWidth;

    // ratio value to angle
    private float mValueRatio;

    // state
    private boolean mIsActive;

    //power of fling
    private float mVelocityRatio;

    //sensitivity of fling
    private float mFlingSensitivity;

    //duration of fling
    private int mFlingDuration;

    //fade in/out effect
    private boolean mIsFade;

    //duration fade affect
    private int mFadeDuration;

    //from value alpha fade animation
    private float mFadeFrom;

    //to value alpha fade animation
    private float mFadeTo;

    //thickness of ring
    private int mThickness;

    private GestureDetector gestureDetector;
    private float mCurrentAngle;
    private float mOldValue;

    //flag user changed value
    private boolean mIsFromUser = false;

    //first touch onScroll
    private boolean mIsFirstOnScroll = false;

    //is now scrolling
    private boolean isNowScrolling;

    private ImageView mImage;
    private Drawable mDrawable;

    private ObjectAnimator mFlingAnim;
    private ObjectAnimator mActiveAnimation;

    private OnValueChangeListener mListener;

    public interface OnValueChangeListener {
        /**
         * Notification that the progress level has changed. Clients can use the
         * fromUser parameter to distinguish user-initiated changes from those
         * that occurred programmatically.
         *
         * @param value
         *          The current value. This will be in the range MIN to MAX
         *
         * @param fromUser
         *          True if the progress change was initiated by the user.
         */
        void onValueChanged(int value, boolean fromUser);

        /**
         * Notification that the progress level has changed and animation is done. Clients can use the
         * fromUser parameter to distinguish user-initiated changes from those
         * that occurred programmatically.
         *
         * @param value
         *          The current value. This will be in the range MIN to MAX
         *
         * @param fromUser
         *          True if the progress change was initiated by the user.
         */
        void onValueChangedDone(int value, boolean fromUser);
    }

    public RingSeekBar(Context context){
        super(context);
        init(context, null, 0, R.style.Theme_RingSeekBarDefaults);
    }

    public RingSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, R.style.Theme_RingSeekBarDefaults);
    }

    public RingSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.Theme_RingSeekBarDefaults);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RingSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        gestureDetector = new GestureDetector(context, new RingGestureListener());

        if (attrs != null) {
            final TypedArray attrArray = getContext().obtainStyledAttributes(attrs, R.styleable.RingSeekBar, defStyleAttr, defStyleRes);
            initAttributes(attrArray);
            attrArray.recycle();
        }

        mImage = new ImageView(context);
        mImage.setImageDrawable(mDrawable);
        setIsFade(mIsFade);
        mImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LayoutParams lp_image = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mImage, lp_image);

        setValue(angleToValue(mCurrentAngle), false);

        mActiveAnimation = ObjectAnimator.ofFloat(mImage, "alpha", mFadeFrom, mFadeTo);
        mActiveAnimation.setDuration(mFadeDuration);
        mActiveAnimation.setInterpolator(new DecelerateInterpolator());

        mFlingAnim = new ObjectAnimator();
        mFlingAnim.setTarget(mImage);
        mFlingAnim.setPropertyName("rotation");
        mFlingAnim.setDuration(mFlingDuration);
        mFlingAnim.setInterpolator(new DecelerateInterpolator());

        setPivotX(mImage.getWidth() / 2);
        setPivotY(mImage.getHeight() / 2);

        mFlingAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentAngle = (float) animation.getAnimatedValue();
                notifyUpdateValue(mIsFromUser, false);
            }
        });

        mFlingAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                playFadeAnimation();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
//                Log.i(TAG, "End fling animation");
                mCurrentAngle = resetMaxMin(mCurrentAngle);
                notifyUpdateValue(mIsFromUser, true);
                if (mIsFade) mActiveAnimation.reverse();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAngle = resetMaxMin(mCurrentAngle);
                notifyUpdateValue(mIsFromUser, false);
                notifyUpdateValue(mIsFromUser, true);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }


    /**
     * Play fade in/fade out animation
     */
    private void playFadeAnimation() {
        if (mIsFade) {
            if (!mActiveAnimation.isRunning()) {
                mActiveAnimation.start();
            } else {
                mActiveAnimation.reverse();
            }
        }
    }

    /**
     * Loads attr from xml
     *
     * @param attrArray array of attributes
     */
    private void initAttributes(TypedArray attrArray) {
        mValueRatio = attrArray.getFloat(R.styleable.RingSeekBar_rsb_valueRatio, DEFAULT_VALUE_RATIO);
        mCurrentAngle = valueToAngle(attrArray.getInteger(R.styleable.RingSeekBar_rsb_value, DEFAULT_VALUE));
        mMaxAngle = valueToAngle(attrArray.getInteger(R.styleable.RingSeekBar_rsb_maxValue, DEFAULT_MAX));
        mMinAngle = valueToAngle(attrArray.getInteger(R.styleable.RingSeekBar_rsb_minValue, DEFAULT_MIN));
        mDrawable = attrArray.getDrawable(R.styleable.RingSeekBar_rsb_srcRing);
        mVelocityRatio = attrArray.getFloat(R.styleable.RingSeekBar_rsb_velocityRatio, DEFAULT_VELOCITY_RATIO) * density;
        mFlingSensitivity = attrArray.getFloat(R.styleable.RingSeekBar_rsb_flingSensitivity, DEFAULT_FLING_SENSITIVITY) * density;
        mFlingDuration = attrArray.getInteger(R.styleable.RingSeekBar_rsb_flingDuration, DEFAULT_FLING_DURATION);
        mIsActive = attrArray.getBoolean(R.styleable.RingSeekBar_rsb_active, DEFAULT_ACTIVE);
        mIsFade = attrArray.getBoolean(R.styleable.RingSeekBar_rsb_fade_in, DEFAULT_FADE_IN);
        mFadeDuration = attrArray.getInteger(R.styleable.RingSeekBar_rsb_fade_duration, DEFAULT_FADE_DURATION);
        mFadeFrom = attrArray.getFloat(R.styleable.RingSeekBar_rsb_fade_from, DEFAULT_FADE_FROM);
        mFadeTo = attrArray.getFloat(R.styleable.RingSeekBar_rsb_fade_to, DEFAULT_FADE_TO);
        mThickness = attrArray.getDimensionPixelSize(R.styleable.RingSeekBar_rsb_thickness, DEFAULT_THICKNESS);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        Bundle state = new Bundle();
        state.putParcelable(PARENT_STATE, superState);

        state.putFloat(CURRENT_ANGLE, mCurrentAngle);
        state.putFloat(MAX_ANGLE, mMaxAngle);
        state.putFloat(MIN_ANGLE, mMinAngle);
        state.putFloat(VALUE_RATIO, mValueRatio);
        state.putBoolean(IS_ACTIVE, mIsActive);
        state.putFloat(VELOCITY_RATIO, mVelocityRatio);
        state.putFloat(FLING_SENSITIVITY, mFlingSensitivity);
        state.putInt(FLING_DURATION, mFlingDuration);
        state.putBoolean(IS_FADE, mIsFade);
        state.putInt(THICKNESS, mThickness);
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle savedState = (Bundle) state;

        Parcelable superState = savedState.getParcelable(PARENT_STATE);
        super.onRestoreInstanceState(superState);

        mCurrentAngle = savedState.getFloat(CURRENT_ANGLE);
        mMaxAngle = savedState.getFloat(MAX_ANGLE);
        mMinAngle = savedState.getFloat(MIN_ANGLE);
        mValueRatio = savedState.getFloat(VALUE_RATIO);
        mIsActive = savedState.getBoolean(IS_ACTIVE);
        mVelocityRatio = savedState.getFloat(VELOCITY_RATIO);
        mFlingSensitivity = savedState.getFloat(FLING_SENSITIVITY);
        mFlingDuration = savedState.getInt(FLING_DURATION);
        mIsFade = savedState.getBoolean(IS_FADE);
        mThickness = savedState.getInt(THICKNESS, mThickness);

        mImage.setRotation(mCurrentAngle);

        mCurrentAngle = resetMaxMin(mCurrentAngle);
        notifyUpdateValue(false, false);
        notifyUpdateValue(false, true);
    }

    /**
     * Converts value to angle
     *
     * @param value value
     * @return angle
     */
    private float valueToAngle(int value) {
        float diff = mCurrentAngle % mValueRatio;
        return value * mValueRatio + diff;
    }

    /**
     * Converts angle to value
     *
     * @param angle angle
     * @return value
     */
    private int angleToValue(float angle) {
        return (int)(angle / mValueRatio);
    }

    /**
     * Sets the current value
     *
     * @param value current value
     * @param isAnimated animated rotation
     */
    public void setValue(int value, boolean isAnimated) {
        if (isNowScrolling) {
            return;
        }

        if (isAnimated) {
            ringAnimationDecelerate(valueToAngle(value) - mCurrentAngle, false);
        } else {
            float angle = valueToAngle(value);
            mCurrentAngle = angle;
            mImage.setRotation(angle);
            mCurrentAngle = resetMaxMin(mCurrentAngle);
            notifyUpdateValue(false, false);
            notifyUpdateValue(false, true);
        }
    }

    /**
     * Set value change listener
     * @param onValueChangeListener listener
     */
    public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
        mListener = onValueChangeListener;
    }

    /**
     * Gets the current value
     */
    public int getValue() {
        return angleToValue(mCurrentAngle);
    }

    /**
     * Sets the max value
     *
     * @param value max value
     */
    public void setMax(int value) {
        mMaxAngle = valueToAngle(value);
    }

    /**
     * Gets the max value
     */
    public int getMax() {
        return angleToValue(mMaxAngle);
    }

    /**
     * Sets the min value
     *
     * @param value min value
     */
    public void setMin(int value) {
        mMinAngle = valueToAngle(value);
    }

    /**
     * Gets the min value
     */
    public int getMin() {
        return angleToValue(mMinAngle);
    }

    /**
     * Gets state
     *
     * @return state
     */
    public boolean isActive() {
        return mIsActive;
    }

    /**
     * Sets state
     *
     * @param isActive state
     */
    public void setActive(boolean isActive) {
        mIsActive = isActive;
    }

    /**
     * Sets power of fling
     *
     * @param ratio power ratio of fling
     */
    public void setVelocityRatio(float ratio) {
        mVelocityRatio = ratio / density;
    }

    /**
     * Gets power of fling
     *
     * @return ratio
     */
    public float getVelocityRatio() {
        return mVelocityRatio * density;
    }

    /**
     * Sets sensitivity of fling
     *
     * @param sensitivity sensitivity of fling
     */
    public void setFlingSensitivity(float sensitivity) {
        mFlingSensitivity = sensitivity / density;
    }

    /**
     * Gets sensitivity of fling
     *
     * @return ratio
     */
    public float getFlingSensitivity() {
        return mFlingSensitivity * density;
    }

    /**
     * Sets fling duration
     *
     * @param duration duration in ms
     */
    public void setFlingDuration(int duration) {
        mFlingDuration = duration;
    }

    /**
     * Sets fling duration
     *
     * @return duration in ms
     */
    public int getFlingDuration() {
        return mFlingDuration;
    }

    /**
     * Gets the value ratio
     */
    public float getValueRatio() {
        return mValueRatio;
    }

    /**
     * Fade animation enable
     */
    public void setIsFade(boolean enable) {
        mIsFade = enable;
        if (enable) mImage.setAlpha(mFadeFrom);
        else mImage.setAlpha(1f);
    }

    /**
     * Set fade from and to alpha value
     * @param fadeFrom value
     * @param fadeTo value
     */
    public void setFadeValues(float fadeFrom, float fadeTo) {
        mFadeFrom = fadeFrom;
        mFadeTo = fadeTo;
        mActiveAnimation.setFloatValues(fadeFrom, fadeTo);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isInRing(event) || !mIsActive) {
                return false;
            }

            if ((mFlingAnim != null) && (mFlingAnim.isRunning())) {
                mFlingAnim.cancel();
            }

            playFadeAnimation();

            mIsFirstOnScroll = true;
            isNowScrolling = true;
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            mCurrentAngle = resetMaxMin(mCurrentAngle);
            notifyUpdateValue(true, true);
            if (mIsFade) mActiveAnimation.reverse();
            isNowScrolling = false;
        }

        return gestureDetector.onTouchEvent(event);
    }

    private class RingGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(velocityX/ mVelocityRatio) + Math.abs(velocityY/ mVelocityRatio) > mFlingSensitivity) {

                float incAngle = Math.abs(velocityX / mVelocityRatio) + Math.abs(velocityY / mVelocityRatio);

                float vX = velocityX / mVelocityRatio;
                float vY = velocityY / mVelocityRatio;
                float x2 = e2.getX() ;
                float y2 = e2.getY() ;

                float angle1 = getTouchAngle(x2, y2);
                float angle2 = getTouchAngle(x2 + vX, y2 + vY);

                float distance = (Math.abs(angle2) - Math.abs(angle1));

                //forward
                if ((angle2 > angle1) && (distance < 180)) {
                    ringAnimationDecelerate(incAngle, true);
                    //back
                } else if ((angle2 < angle1) && (-distance < 180)) {
                    ringAnimationDecelerate(-incAngle, true);
                    //forward over 0
                } else if ((angle2 > angle1) && (distance > 180)) {
                    ringAnimationDecelerate(-incAngle, true);
                    //back over 0
                } else if ((angle2 < angle1) && (-distance > 180)) {
                    ringAnimationDecelerate(incAngle, true);
                }
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            if (e2.getAction() == MotionEvent.ACTION_MOVE) {
                if (mIsFirstOnScroll) {
                    mIsFirstOnScroll = false;
                    return true;
                }

                float angle1 = getTouchAngle(e2.getX(), e2.getY());
                float angle2 = getTouchAngle(e2.getX() + distanceX, e2.getY() + distanceY);

                float distance = (Math.abs(angle2) - Math.abs(angle1));

                //forward
                if ((angle2 > angle1) && (distance < 180)) {
                    ringScrollAnimation(angle1 - angle2);
                    //back
                } else if ((angle2 < angle1) && (-distance < 180)) {
                    ringScrollAnimation(angle1 - angle2);
                    //forward over 0
                } else if ((angle2 > angle1) && (distance > 180)) {
                    ringScrollAnimation(360 - angle1 - angle2);
                    //back over 0
                } else if ((angle2 < angle1) && (-distance > 180)) {
                    ringScrollAnimation(360 - angle1 - angle2);
                }
            }
            return true;
        }
    }

    /**
     * Calculate angle
     *
     * @param globalX global X coordinate
     * @param globalY global Y coordinate
     *
     * @return angle of coordinate
     */
    private float getTouchAngle(float globalX, float globalY) {
        float touchAngle = (float) ((Math.atan2(globalY - mImage.getHeight()/2, globalX - mImage.getWidth()/2) / Math.PI * 180) % 360);
        return (touchAngle < 0 ? 360 + touchAngle : touchAngle);
    }

    /**
     * Check the position in the circle
     *
     * @param event touch event
     * @return in circle
     */
    private boolean isInRing(MotionEvent event) {
        float centerX = mWidth / 2;
        float centerY = mHeight / 2;
        float x = event.getX() - centerX;
        float y = event.getY() - centerY;
        float minSide = (centerX - centerY) < 0 ? mWidth : mHeight;
        float outerRadius = minSide / 2;
        float innerRadius = minSide / 2 - mThickness;
        float touchEventRadius = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));

        return ((touchEventRadius <= outerRadius) && (touchEventRadius >= innerRadius));
    }

    /**
     * Animation on fling
     *
     * @param inc rotation increment
     */
    private void ringAnimationDecelerate(float inc, boolean fromUser) {
        mIsFromUser = fromUser;
        int endValue = (int)(mCurrentAngle + inc) / (int)mValueRatio * (int)mValueRatio;
        mFlingAnim.setFloatValues(mCurrentAngle, endValue);
        mFlingAnim.start();
    }

    /**
     * Rotate image
     *
     * @param inc rotation increment
     */
    private void ringScrollAnimation(float inc) {
        mCurrentAngle += inc;
        mImage.setRotation(mCurrentAngle);
        notifyUpdateValue(true, false);
    }

    /**
     * Notify value changed
     *
     * @param fromUser is user interaction
     * @param isActionDone type of event
     */
    private void notifyUpdateValue(boolean fromUser, boolean isActionDone) {
        float angle = resetMaxMin(mCurrentAngle);
        int value = angleToValue(angle);

        if (value == mOldValue) return;
        if (mListener == null) return;

        if (isActionDone) {
            mListener.onValueChangedDone(value, fromUser);
        } else {
            mListener.onValueChanged(value, fromUser);
        }

        mOldValue = angleToValue(angle);
    }

    /**
     * Reset angle to max min bounds
     *
     * @param angle angle
     */
    private float resetMaxMin(float angle) {
        angle = (angle > mMaxAngle) ? mMaxAngle : angle;
        angle = (angle < mMinAngle) ? mMinAngle : angle;
        return angle;
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mHeight = getMeasuredHeight();
        mWidth = getMeasuredWidth();
    }
}
