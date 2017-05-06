package com.android.tyw.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

/**
 * Created by tangyiwu on 2017/4/28.
 */

public class DiscreteSeekBar extends View {
    private static final int DEFAULT_SEEK_BAR_HEIGHT_DP = 8;
    private static final int DEFAULT_TEXT_SIZE_DP = 14;
    private static final int DEFAULT_TEXT_MARGIN_DP = 10;
    private static final int DEFAULT_DURATION = 300;
    private static final int DEFAULT_TEXT_COLOR = Color.parseColor("#999999");
    private Scroller mScroller;
    private int mDuration;
    private Paint mTextPaint;
    private Paint.FontMetrics mFontMetrics;
    private int mSeekBarHeight;
    private int mTextSize;
    private int mTextColor;
    private int mTextMargin;
    private Drawable mBackground;
    private Drawable mProgressBackground;
    private Drawable mDivider;
    private Drawable mCursor;
    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;
    private int mCursorPosition;//游标所在的位置
    private String[] data;
    private int mTextHeight;
    private float mDownX;//手指触摸屏幕的x轴坐标
    private int mTouchSlop;
    private int mCursorX;//游标所在的x轴坐标
    private boolean isCursorSlide;//游标是否在滑动

    public interface OnSeekBarSelectedListener {
        void onSelect(int position);
    }

    private OnSeekBarSelectedListener mListener;

    public DiscreteSeekBar(Context context) {
        this(context, null);
    }

    public DiscreteSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DiscreteSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseConfig(context, attrs);
        initPaint();
        mScroller = new Scroller(context, new DecelerateInterpolator());
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    /**
     * 解析属性参数配置
     */
    private void parseConfig(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DiscreteSeekBar);
        mSeekBarHeight = (int) a.getDimension(R.styleable.DiscreteSeekBar_dsb_height, dp2px(context, DEFAULT_SEEK_BAR_HEIGHT_DP));
        mBackground = a.getDrawable(R.styleable.DiscreteSeekBar_dsb_background);
        mProgressBackground = a.getDrawable(R.styleable.DiscreteSeekBar_dsb_progressBackground);
        mDivider = a.getDrawable(R.styleable.DiscreteSeekBar_dsb_divider);
        mCursor = a.getDrawable(R.styleable.DiscreteSeekBar_dsb_cursor);
        mTextColor = a.getColor(R.styleable.DiscreteSeekBar_dsb_textColor, DEFAULT_TEXT_COLOR);
        mTextSize = (int) a.getDimension(R.styleable.DiscreteSeekBar_dsb_textSize, dp2px(context, DEFAULT_TEXT_SIZE_DP));
        mTextMargin = (int) a.getDimension(R.styleable.DiscreteSeekBar_dsb_textMargin, dp2px(context, DEFAULT_TEXT_MARGIN_DP));
        mDuration = a.getInteger(R.styleable.DiscreteSeekBar_dsb_duration, DEFAULT_DURATION);
        mPaddingLeft = getPaddingLeft();
        mPaddingTop = getPaddingTop();
        mPaddingRight = getPaddingRight();
        mPaddingBottom = getPaddingBottom();
        mCursorX = mPaddingLeft;
        if (mBackground == null) {
            mBackground = ContextCompat.getDrawable(context, R.drawable.bg_discrete_seekbar);
        }
        if (mProgressBackground == null) {
            mProgressBackground = ContextCompat.getDrawable(context, R.drawable.bg_discrete_seekbar_progress);
        }
        if (mDivider == null) {
            mDivider = ContextCompat.getDrawable(context, R.drawable.divider_discrete_seekbar);
        }
        if (mCursor == null) {
            mCursor = ContextCompat.getDrawable(context, R.drawable.cursor_discrete_seekbar);
        }
        a.recycle();
    }

    private void initPaint() {
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setStrokeWidth(1.0f);
        mFontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = (int) (mFontMetrics.descent - mFontMetrics.ascent);
    }

    /**
     * 设置数据
     */
    public void setData(String[] data) {
        this.data = data;
        invalidate();
    }

    /**
     * 设置当前索引位置
     */
    public void setSelect(final int position) {
        if (data == null) {
            throw new NullPointerException("the data of DiscreteSeekBar is null");
        }
        if (position < 0 || position >= data.length) {
            throw new ArrayIndexOutOfBoundsException(position + " is out of range " + data.length);
        }
        post(new Runnable() {
            @Override
            public void run() {
                mCursorPosition = position;
                mCursorX = calcX(position);
                invalidate();
            }
        });
    }

    /**
     * set select listener
     */
    public void setOnSeekBarSelectedListener(OnSeekBarSelectedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void setPadding(@Px int left, @Px int top, @Px int right, @Px int bottom) {
        super.setPadding(left, top, right, bottom);
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightNeed = mSeekBarHeight + mPaddingTop + mPaddingBottom + mTextHeight + mTextMargin;
        if (heightMode == MeasureSpec.EXACTLY) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        } else {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightNeed, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawProgressBackground(canvas);
        drawCursor(canvas);
        drawText(canvas);
    }

    /**
     * 绘制底部背景
     */
    private void drawBackground(Canvas canvas) {
        int left = mPaddingLeft;
        int top = mPaddingTop;
        int right = getMeasuredWidth() - mPaddingRight;
        int bottom = top + mSeekBarHeight;
        Rect bgRect = new Rect(left, top, right, bottom);
        mBackground.setBounds(bgRect);
        mBackground.draw(canvas);
    }

    /**
     * 绘制选中进度背景
     */
    private void drawProgressBackground(Canvas canvas) {
        int left = mPaddingLeft;
        int top = mPaddingTop;
        int right = mCursorX;
        int bottom = top + mSeekBarHeight;
        Rect progressBgRect = new Rect(left, top, right, bottom);
        mProgressBackground.setBounds(progressBgRect);
        mProgressBackground.draw(canvas);
    }

    /**
     * 绘制游标
     */
    private void drawCursor(Canvas canvas) {
        if (data == null || data.length == 0) {
            drawCursorSelected(canvas);
            return;
        }
        for (int i = 1; i < data.length - 1; i++) {
            drawCursorUnselected(canvas, i);
        }
        drawCursorSelected(canvas);
    }

    private void drawCursorSelected(Canvas canvas) {
        int centerX = mCursorX;
        int centerY = (int) (mPaddingTop + 1.0f * mSeekBarHeight / 2);
        int cursorWidth = mCursor.getIntrinsicWidth();
        int cursorHeight = mCursor.getIntrinsicHeight();
        int left = (int) (centerX - cursorWidth * 1.0f / 2);
        int right = left + cursorWidth;
        int top = (int) (centerY - cursorHeight * 1.0f / 2);
        int bottom = top + cursorHeight;
        Rect rect = new Rect(left, top, right, bottom);
        mCursor.setBounds(rect);
        mCursor.draw(canvas);
    }

    private void checkCursor() {
        if (mCursorX < mPaddingLeft) {
            mCursorX = mPaddingLeft;
            return;
        }
        if (mCursorX > getMeasuredWidth() - mPaddingRight) {
            mCursorX = getMeasuredWidth() - mPaddingRight;
        }
    }

    private void drawCursorUnselected(Canvas canvas, int position) {
        int centerX = calcX(position);
        int centerY = (int) (mPaddingTop + 1.0f * mSeekBarHeight / 2);
        int cursorWidth = mDivider.getIntrinsicWidth();
        int cursorHeight = mDivider.getIntrinsicHeight();
        int left = (int) (centerX - cursorWidth * 1.0f / 2);
        int right = left + cursorWidth;
        int top = (int) (centerY - cursorHeight * 1.0f / 2);
        int bottom = top + cursorHeight;
        Rect rect = new Rect(left, top, right, bottom);
        mDivider.setBounds(rect);
        mDivider.draw(canvas);
    }

    /**
     * 绘制文字
     */
    private void drawText(Canvas canvas) {
        if (data != null && data.length > 0) {
            float baseline = getMeasuredHeight() - mPaddingBottom - mFontMetrics.descent;
            for (int i = 0; i < data.length; i++) {
                int x = calcX(i);
                canvas.drawText(data[i], x, baseline, mTextPaint);
            }
        }
    }

    /**
     * 计算索引位置X坐标
     */
    private int calcX(int position) {
        if (data == null || data.length < 1) {
            return mPaddingLeft;
        }
        float step = getMeasuredWidth() * 1.0f - mPaddingLeft - mPaddingRight;
        if (data.length > 1) {
            step = (getMeasuredWidth() * 1.0f - mPaddingLeft - mPaddingRight) / (data.length - 1);
        }
        return (int) (mPaddingLeft + step * position);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        mDownX = event.getX();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mCursorX = (int) mDownX;
                checkCursor();
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                mCursorX = (int) mDownX;
                checkCursor();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                mCursorPosition = calcCursorPosition();
                int slideX = calcX(mCursorPosition);
                int delta = (int) (slideX - mDownX);
                if (shouldSlide(delta)) {
                    isCursorSlide = true;
                    mScroller.startScroll((int) mDownX, 0, delta, 0, mDuration);
                    invalidate();
                } else {
                    mCursorX = slideX;
                    invalidate();
                    if (mListener != null) {
                        mListener.onSelect(mCursorPosition);
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mCursorX = mScroller.getCurrX();
            invalidate();
        } else {
            if (isCursorSlide && mListener != null) {
                mListener.onSelect(mCursorPosition);
                isCursorSlide = false;
            }
        }
    }

    /**
     * 计算游标应该停在的位置
     */
    private int calcCursorPosition() {
        if (data == null || data.length == 0) {
            return 0;
        }
        float step = getMeasuredWidth() * 1.0f - mPaddingLeft - mPaddingRight;
        if (data.length > 1) {
            step = (getMeasuredWidth() * 1.0f - mPaddingLeft - mPaddingRight) / (data.length - 1);
        }
        int leftPosition = (int) ((mDownX - mPaddingLeft) / step);
        int rightPosition = leftPosition + 1;
        int leftX = calcX(leftPosition);
        int rightX = calcX(rightPosition);
        return Math.abs(leftX - mDownX) < Math.abs(rightX - mDownX) ? leftPosition : rightPosition;
    }

    /**
     * 是否应该滑动
     */
    private boolean shouldSlide(int distance) {
        return Math.abs(distance) > mTouchSlop;
    }

    private int dp2px(Context context, int dpValue) {
        return (int) (context.getResources().getDisplayMetrics().density * dpValue + 0.5f);
    }
}
