package com.sc.github.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * 爆炸效果点赞View
 *
 * @author shamschu
 * @Date 17/11/8 上午11:37
 */
public class BoomLikeView extends View {

    private static final String TAG = BoomLikeView.class.getName();
    private Paint mPaint;
    private int mLikedResId;
    private int mUnlikeResId;
    private Bitmap mLikedBitmap;
    private Bitmap mUnlikeBitmap;
    private float mBitmapScale = 1.0f;
    private float mHeartDistanceRatio = 0.0f;
    private PathPointInfo[] mPoints = new PathPointInfo[7];
    private boolean mMeasurePoints = false;
    private boolean mMeasurePaths = false;
    private Path[] mPaths = new Path[7];
    private PathMeasure mPathMeasure = new PathMeasure();
    private float[] pos = new float[2];
    private float mFireworkScale = 1.0f;
    private boolean mLike = false;
    private Path mFireworkPath = new Path();
    private int mFireworkHeartOffset = 15;
    private int mSrcWidth;
    private int mSrcHeight;
    private int mFireworkColor;
    private boolean mSetSrc = false;
    private boolean mPreparePlayAnim = true;
    private int mHeartScaleAnimTime = 500;
    private int mFireworkAnimTime = 1000;
    private ObjectAnimator mHeartScaleAnimator;
    private ObjectAnimator mFireworkMoveAnimator;
    private ObjectAnimator mFireworkScaleAnimator;
    private ObjectAnimator mUnlikeAnimator;
    private AnimatorSet mFireworkSet;
    private String mText;
    private int mTextColor;
    private int mTextSize;
    private int mTextPadding;

    public BoomLikeView(Context context) {
        this(context, null);
    }

    public BoomLikeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BoomLikeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        initAttrs(attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        RectF rectF = new RectF(0, 0, mFireworkHeartOffset, mFireworkHeartOffset);
        RectF rectF1 = new RectF(mFireworkHeartOffset, 0, mFireworkHeartOffset * 2, mFireworkHeartOffset);
        mFireworkPath.addArc(rectF, -225, 225);
        mFireworkPath.arcTo(rectF1, -180, 225, false);
        mFireworkPath.lineTo(mFireworkHeartOffset, mFireworkHeartOffset + 10);
        for (int i = 0; i < mPoints.length; i++) {
            mPoints[i] = new PathPointInfo();
        }
    }

    private void initAttrs(AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.BoomLikeView);
        try {
            mUnlikeResId = array.getResourceId(R.styleable.BoomLikeView_src, 0);
            mFireworkColor = array.getColor(R.styleable.BoomLikeView_fireworkColor, Color.parseColor("#F9D1D1"));
            mHeartScaleAnimTime = array.getInt(R.styleable.BoomLikeView_srcAnimTime, 500);
            mFireworkAnimTime = array.getInt(R.styleable.BoomLikeView_fireworkAnimTime, 500);
            mText = array.getString(R.styleable.BoomLikeView_text);
            mTextPadding = array.getDimensionPixelOffset(R.styleable.BoomLikeView_textPadding, 5);
            mTextColor = array.getColor(R.styleable.BoomLikeView_textColor, Color.parseColor("#bdbdbd"));
            mTextSize = array.getDimensionPixelOffset(R.styleable.BoomLikeView_textSize, sp2px(getContext(), 12));
        } finally {
            array.recycle();
        }
        mSetSrc = mUnlikeResId > 0;
        if (mSetSrc) {
            setSrc(mUnlikeResId);
        }
    }

    /**
     * 设置点赞后的资源图
     *
     * @param likedResId 点赞后的资源图
     */
    public void setLikedImageResource(int likedResId) {
        if (!mSetSrc) {
            Log.d(TAG, "Can not invoke setLikedImageResource(), you need invoke setSrc(int resId) or use app:src in the layout xml!");
            return;
        }
        if (mLikedResId == likedResId) {
            return;
        }
        this.mLikedResId = likedResId;
        mLikedBitmap = BitmapFactory.decodeResource(getResources(), mLikedResId);
        requestLayout();
    }

    /**
     * 设置初始资源图，一般为未点赞的图
     *
     * @param unlikeResId 初始图，一般为未点赞的图
     */
    public void setSrc(int unlikeResId) {
        if (mUnlikeResId == unlikeResId) {
            return;
        }
        this.mUnlikeResId = unlikeResId;
        mUnlikeBitmap = BitmapFactory.decodeResource(getResources(), mUnlikeResId);
        mSrcWidth = mUnlikeBitmap.getWidth();
        mSrcHeight = mUnlikeBitmap.getHeight();
        mSetSrc = true;
        requestLayout();
    }

    /**
     * 设置烟花心的填充颜色
     *
     * @param fireworkColor 烟花心的填充颜色
     */
    public void setFireworkColor(int fireworkColor) {
        this.mFireworkColor = fireworkColor;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        widthMeasureSpec = MeasureSpec.makeMeasureSpec((int) (mSrcWidth * 3.0f) + mFireworkHeartOffset * 2, MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec((int) (mSrcHeight * 3.5f) + +mFireworkHeartOffset * 2, MeasureSpec.EXACTLY);
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!mSetSrc) {
            return;
        }
        int centerX = getWidth() >> 1;
        int centerY = getHeight() >> 1;
        resetPoints(centerX, centerY);
        resetPointsPath(centerX, centerY);
        int x = centerX - mSrcWidth / 2;
        int y = centerY - mSrcHeight / 2;
        if (!TextUtils.isEmpty(mText)) {
            mPaint.setColor(mTextColor);
            mPaint.setTextSize(mTextSize);
            mPaint.setFakeBoldText(mTextBold);
            float tw = mPaint.measureText(mText);
            float tx = x - tw - mTextPadding;
            float ty = centerY - (mPaint.ascent() + mPaint.descent()) / 2;
            canvas.drawText(mText, tx, ty, mPaint);
        }
        if (!mLike) {
            canvas.save();
            canvas.scale(mBitmapScale, mBitmapScale, centerX, centerY);
            canvas.drawBitmap(mUnlikeBitmap, x, y, mPaint);
            canvas.restore();
            return;
        }
        if (mHeartDistanceRatio > 0) {
            for (Path path : mPaths) {
                mPathMeasure.setPath(path, false);
                mPathMeasure.getPosTan(mPathMeasure.getLength() * mHeartDistanceRatio, pos, null);
                mPaint.setAlpha((int) (255 * mFireworkScale));
                canvas.save();
                float bx = pos[0] - mLikedBitmap.getWidth() / 2;
                float by = pos[1] - mLikedBitmap.getHeight() / 2;
                canvas.scale(mFireworkScale, mFireworkScale, pos[0], pos[1]);
                canvas.translate(bx, by);
                mPaint.setColor(mFireworkColor);
                canvas.drawPath(mFireworkPath, mPaint);
                canvas.restore();
            }
        }
        mPaint.setAlpha(255);
        canvas.save();
        canvas.scale(mBitmapScale, mBitmapScale, centerX, centerY);
        canvas.drawBitmap(mLikedBitmap, x, y, mPaint);
        canvas.restore();
    }

    public void setFireworkScale(float fireworkScale) {
        this.mFireworkScale = fireworkScale;
        invalidate();
    }

    /**
     * 获取是否点赞
     *
     * @return 是否点赞
     */
    public boolean isLike() {
        return mLike;
    }

    /**
     * 设置点赞状态
     *
     * @param like 设置点赞状态
     */
    public boolean setLike(boolean like) {
        if (mLike == like) {
            return false;
        }
        this.mLike = like;
        invalidate();
        return true;
    }

    /**
     * 设置点赞状态并播放动画
     *
     * @param isLike 要设置的点赞状态，true点赞/false取消点赞
     */
    public boolean play(boolean isLike) {
        if (isLike) {
            return like();
        } else {
            return unLike();
        }
    }

    /**
     * 设置点赞状态并判断是否需要播放动画
     *
     * @param isLike     要设置的点赞状态，true点赞/false取消点赞
     * @param isPlayAnim 是否要播放动画, true播放/false不播放
     */
    public boolean play(boolean isLike, boolean isPlayAnim) {
        if (isPlayAnim) {
            return play(isLike);
        } else {
            return setLike(isLike);
        }
    }

    /**
     * 是否可以播放下一个动画
     *
     * @return 是否可以播放下一个动画， true可播放/false不可播放
     */
    public boolean isPrepare() {
        return mPreparePlayAnim;
    }

    private void initLikesAnimators() {
        if (mHeartScaleAnimator == null) {
            mHeartScaleAnimator = ObjectAnimator.ofFloat(this, "bitmapScale", 0f, 1.0f);
            mHeartScaleAnimator.setInterpolator(new OvershootInterpolator());
            mHeartScaleAnimator.setDuration(mHeartScaleAnimTime);
        }
        if (mFireworkMoveAnimator == null) {
            mFireworkMoveAnimator = ObjectAnimator.ofFloat(this, "heartDistanceRatio", 0f, 1.0f);
        }
        if (mFireworkScaleAnimator == null) {
            mFireworkScaleAnimator = ObjectAnimator.ofFloat(this, "fireworkScale", 0f, 1.0f, 0f);
        }
        if (mFireworkSet == null) {
            mFireworkSet = new AnimatorSet();
            mFireworkSet.playTogether(mFireworkMoveAnimator, mFireworkScaleAnimator);
            mFireworkSet.setDuration(mFireworkAnimTime);
            mFireworkSet.setInterpolator(new DecelerateInterpolator());
            mFireworkSet.addListener(new PrepareAnimListener());
        }
    }

    private void initUnlikeAnimators() {
        if (mUnlikeAnimator == null) {
            mUnlikeAnimator = ObjectAnimator.ofFloat(this, "bitmapScale", 1.0f, 1.5f, 1.0f);
            mUnlikeAnimator.setDuration(mHeartScaleAnimTime);
            mUnlikeAnimator.setInterpolator(new DecelerateInterpolator());
            mUnlikeAnimator.addListener(new PrepareAnimListener());
        }
    }

    /**
     * 点赞并播放点赞动画
     */
    public boolean like() {
        if (!mPreparePlayAnim) {
            return false;
        }
        if (mLikedResId <= 0) {
            Log.d(TAG, "Liked resource id has not been set, you need to call setLikedImageResource() first!");
            return false;
        }
        mPreparePlayAnim = false;
        mLike = true;
        initLikesAnimators();
        mHeartScaleAnimator.start();
        mFireworkSet.start();
        return true;
    }

    /**
     * 取消点赞并播放取消点赞动画
     */
    public boolean unLike() {
        if (!mPreparePlayAnim) {
            return false;
        }
        mPreparePlayAnim = false;
        mLike = false;
        initUnlikeAnimators();
        mUnlikeAnimator.start();
        return true;
    }

    /**
     * 设置文字内容
     *
     * @param text 文字内容
     */
    public void setText(String text) {
        this.mText = text;
        invalidate();
    }

    /**
     * 设置文字颜色
     *
     * @param textColor 文字颜色
     */
    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
        invalidate();
    }

    /**
     * 设置文字大小
     *
     * @param textSize 文字大小
     */
    public void setTextSize(int textSize) {
        this.mTextSize = textSize;
        invalidate();
    }

    /**
     * 设置文字是否加粗
     *
     * @param isBold 是否加粗
     */
    public void setTextBold(boolean isBold) {
        this.mTextBold = isBold;
        invalidate();
    }

    private boolean mTextBold = false;

    /**
     * 设置文字和图片间隔
     *
     * @param textPadding 文件和图片间隔
     */
    public void setTextPadding(int textPadding) {
        this.mTextPadding = textPadding;
        invalidate();
    }

    public void setHeartDistanceRatio(float heartDistanceRatio) {
        this.mHeartDistanceRatio = heartDistanceRatio;
        invalidate();
    }

    public void setBitmapScale(float scale) {
        this.mBitmapScale = scale;
        invalidate();
    }

    private void resetPoints(int centerX, int centerY) {
        if (mMeasurePoints) {
            return;
        }
        mMeasurePoints = true;
        int halfBitmapWidth = mUnlikeBitmap.getWidth() >> 1;
        int halfBitmapHeight = mUnlikeBitmap.getHeight() >> 1;

        int left = centerX - halfBitmapWidth;
        int right = centerX + halfBitmapHeight;
        int top = centerY - halfBitmapHeight;
        int bottom = centerY + halfBitmapHeight;

        int endX1 = left - halfBitmapWidth;
        int endY1 = top - 2 * halfBitmapHeight;
        int ctrlX1 = endX1 - 5;
        int ctrlY1 = centerY - 5;

        int endX2 = centerX - 5;
        int endY2 = top - 3 * halfBitmapHeight;
        int ctrlX2 = endX2 - 10;
        int ctrlY2 = top - halfBitmapHeight;

        int endX3 = right + halfBitmapWidth;
        int endY3 = endY1;
        int ctrlX3 = endX3 + 5;
        int ctrlY3 = top - halfBitmapHeight;

        int endX4 = left - 2 * halfBitmapWidth;
        int endY4 = top - 10;
        int ctrlX4 = endX4 + 10;
        int ctrlY4 = bottom + 5;

        int endX5 = right + 2 * halfBitmapWidth;
        int endY5 = top - halfBitmapHeight;
        int ctrlX5 = endX5 - 10;
        int ctrlY5 = bottom;

        int endX6 = left - halfBitmapWidth;
        int endY6 = bottom - halfBitmapHeight;
        int ctrlX6 = endX6 + 10;
        int ctrlY6 = bottom + 2 * halfBitmapHeight;

        int endX7 = right + 2 * halfBitmapWidth;
        int endY7 = bottom - halfBitmapHeight;
        int ctrlX7 = right;
        int ctrlY7 = bottom + 2 * halfBitmapHeight;

        mPoints[0].set(endX1, endY1, ctrlX1, ctrlY1);
        mPoints[1].set(endX2, endY2, ctrlX2, ctrlY2);
        mPoints[2].set(endX3, endY3, ctrlX3, ctrlY3);
        mPoints[3].set(endX4, endY4, ctrlX4, ctrlY4);
        mPoints[4].set(endX5, endY5, ctrlX5, ctrlY5);
        mPoints[5].set(endX6, endY6, ctrlX6, ctrlY6);
        mPoints[6].set(endX7, endY7, ctrlX7, ctrlY7);
    }

    private void resetPointsPath(int centerX, int centerY) {
        if (mMeasurePaths) {
            return;
        }
        mMeasurePaths = true;
        for (int i = 0; i < mPoints.length; i++) {
            PathPointInfo pointInfo = mPoints[i];
            Path path = new Path();
            path.moveTo(centerX, centerY);
            path.quadTo(pointInfo.controlX, pointInfo.controlY, pointInfo.endX, pointInfo.endY);
            mPaths[i] = path;
        }
    }

    public class PathPointInfo {

        int controlX;
        int controlY;
        int endX;
        int endY;

        void set(int endX, int endY, int controlX, int controlY) {
            this.controlX = controlX;
            this.controlY = controlY;
            this.endX = endX;
            this.endY = endY;
        }
    }

    private class PrepareAnimListener implements Animator.AnimatorListener {
        @Override
        public void onAnimationStart(Animator animator) {
            mPreparePlayAnim = false;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            mPreparePlayAnim = true;
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            mPreparePlayAnim = true;
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            mPreparePlayAnim = false;
        }
    }

    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

}