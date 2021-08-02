package com.lhw.floatingwindow.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lhw.floatingwindow.R;
import com.lhw.floatingwindow.utils.Func;
import com.lhw.floatingwindow.viewinterface.FloatClickListener;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.WINDOW_SERVICE;

/**
 * ****************************************************************
 * 文件名称 : com.ziwenl.floatingwindowdemo.widgets.FloatingView
 * 作    者 : lhw
 * 创建时间 : 2021/06/28 14:40
 * 文件描述 : 通过view的方式实现悬浮窗
 * 版权声明 : Copyright (C) 2015-2018 杭州中焯信息技术股份有限公司
 * 修改历史 : 2021/06/28  1.00 初始版本
 * ****************************************************************
 */
public class FloatingView extends View {
    private Context mContext;
    /**
     * 默认宽高与当前View实际宽高
     */
    private int mDefaultWidth, mDefaultHeight;
    private int mWidth, mHeight;
    /**
     * 当前View绘制相关
     */
    private Paint mPaint;
    private Bitmap mBitmap;
    private PorterDuffXfermode mPorterDuffXfermode;
    private FloatingView.Direction mDirection = FloatingView.Direction.right;
    private FloatingView.Direction mPartDirection = FloatingView.Direction.right;//滑动前停靠的方位
    private int mOrientation;
    private int mWidthPixels;
    private int mHeightPixels;

    private int mDefaultPositionY = 1300;

    //间隔和圆角
    private int d;
    //关闭按钮宽度
    private int closeWidth;

    //记录down/up位置
    private float xUpScreen;
    private float yUpScreen;
    private float xDownInScreen;
    private float yDownInScreen;

    //为了绘制阴影，设置一个边距
    private int mPadding;
    private int mShadowRadius;//阴影半径
    private int mShadowDx;//阴影横向偏移
    private int mShadowDy;//阴影纵向偏移

    /**
     * 图片内容区域的尺寸
     */
    private int mContentSize;
    /**
     * 图片最多数量
     */
    public static final int MAX_DRAWABLE_COUNT = 5;
    /**
     * 图片间隙
     */
    public static final float DEFAULT_GAP = 0.17f;

    private final List<DrawableInfo> mDrawables = new ArrayList<>(MAX_DRAWABLE_COUNT);
    private final Paint mImageGroupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mLayoutMatrix = new Matrix();
    private final RectF mTempBounds = new RectF();
    private final float[] mPointsTemp = new float[2];

    /**
     * 小圆半径
     */
    private float mSteinerCircleRadius;
    /**
     * 整体相对Y轴的偏移量
     */
    private float mOffsetY;

    private FloatingView.FitType mFitType = FloatingView.FitType.CENTER;
    private float mGap = DEFAULT_GAP;

    private int mCloseImageSize;//关闭按钮尺寸
    //上下边距
    public static final int MARGIN_EDGE_TOP = 50;
    public static final int MARGIN_EDGE_BOTTOM = 100;

    private float mOriginalRawX;
    private float mOriginalRawY;
    private float mOriginalX;
    private float mOriginalY;

    private static final FloatingView.FitType[] sFitTypeArray = {
            FloatingView.FitType.FIT,
            FloatingView.FitType.CENTER,
            FloatingView.FitType.START,
            FloatingView.FitType.END,
    };
    private FloatClickListener mMagnetViewListener;

    protected MoveAnimator mMoveAnimator;

    public FloatingView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public FloatingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public FloatingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        d = Func.dp2px(mContext, 7);
        closeWidth = Func.dp2px(mContext, 32);
        mPadding = Func.dp2px(mContext, 5);
        mCloseImageSize = Func.dp2px(mContext, 12);
        mShadowRadius = Func.dp2px(mContext, 3);
        mShadowDx = Func.dp2px(mContext, 0);
        mShadowDy = Func.dp2px(mContext, 1);

        mMoveAnimator = new MoveAnimator();
        //当前View绘制相关
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPorterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP);
        mBitmap = Func.drawable2Bitmap((BitmapDrawable) getResources().getDrawable(R.drawable.close), mCloseImageSize);
        mDefaultHeight = Func.dp2px(mContext, 55);
        mDefaultWidth = mDefaultHeight + closeWidth;
        //记录当前屏幕方向和屏幕宽度
        recordScreenWidth();
        //组合图片
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        mImageGroupPaint.setColor(Color.BLACK);
        mImageGroupPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        initForEditMode();

        //初始位置
        setX(mWidthPixels - mDefaultWidth);
        setY(mDefaultPositionY);
        mDirection = FloatingView.Direction.right;
        invalidate();
    }

    /**
     * 记录当前屏幕方向和屏幕宽度
     */
    private void recordScreenWidth() {
        WindowManager mWindowManager = (WindowManager) mContext.getSystemService(WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(outMetrics);
        mOrientation = getResources().getConfiguration().orientation;
        mWidthPixels = outMetrics.widthPixels;
        mHeightPixels = outMetrics.heightPixels;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = mDefaultWidth;
        mHeight = mDefaultHeight;
        mContentSize = mHeight - d * 2;
        setMeasuredDimension(mWidth + mPadding * 2, mHeight + mPadding * 2);
    }

    /**
     * 处理触摸事件，实现拖动、形状变更和粘边效果
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getResources().getConfiguration().orientation != mOrientation) {
            //屏幕方向翻转了，重新获取并记录屏幕宽度
            recordScreenWidth();
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mMoveAnimator.stop();
                xDownInScreen = event.getRawX();
                yDownInScreen = event.getRawY();
                xUpScreen = xDownInScreen;
                yUpScreen = yDownInScreen;
                mOriginalX = getX();
                mOriginalY = getY();
                mOriginalRawX = event.getRawX();
                mOriginalRawY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                xUpScreen = event.getRawX();
                yUpScreen = event.getRawY();
                updateViewPosition(event);
                if (mDirection != FloatingView.Direction.move) {
                    mDirection = FloatingView.Direction.move;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                handleDirection((int) event.getRawX(), (int) event.getRawY());
                invalidate();
                if (isOnClickEvent()) {
                    //判断是否点击关闭按钮
                    if (mDirection == FloatingView.Direction.right && xUpScreen > mWidthPixels - closeWidth
                            || mDirection == FloatingView.Direction.left && xUpScreen < closeWidth) {
                        setVisibility(GONE);
                    } else {
                        dealClickEvent();
                    }
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        int r = mHeight / 2;
        //画透明色圆角背景
        mPaint.setColor(Color.WHITE);
        final List<DrawableInfo> drawables = mDrawables;
        final int N = drawables.size();
        int drawableWidth = mContentSize - getPaddingLeft() - getPaddingRight();
        int drawableHeight = mContentSize - getPaddingTop() - getPaddingBottom();
        final Paint paint = mImageGroupPaint;
        //包含间隙的圆半径
        final float gapRadius = mSteinerCircleRadius * (mGap + 1f);
        Path mPath = new Path();

        //根据最后停留方向（left or right）绘制多一层直角矩形，覆盖圆角
        switch (mDirection) {
            default:
            case right:
                canvas.translate(mPadding, mPadding);
                //绘制圆角矩形
                //float[] array = {leftTopRound, leftTopRound, rightTopRound, rightTopRound, rightBottomRound, rightBottomRound, leftBottomRound, leftBottomRound};
                float[] arrayR = {r, r, 0, 0, 0, 0, r, r};
                mPath.addRoundRect(new RectF(0, 0, (float) mWidth, (float) mHeight), arrayR, Path.Direction.CW);
                mPaint.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, Color.GRAY);
                canvas.drawPath(mPath, mPaint);
                mPaint.clearShadowLayer();

                //绘制关闭的小叉
                Rect mSrcRectR = new Rect(0, 0, mCloseImageSize, mCloseImageSize);
                int leftR = mWidth - ((closeWidth - mCloseImageSize) / 2 + mCloseImageSize) - mPadding;
                int topR = mHeight / 2 - mCloseImageSize / 2;
                Rect mDestRectR = new Rect(leftR, topR, leftR + mCloseImageSize, topR + mCloseImageSize);
                canvas.drawBitmap(mBitmap, mSrcRectR, mDestRectR, mPaint);

                //绘制图片组合
                if (!isInEditMode() && (mContentSize <= 0 || N <= 0)) {
                    return;
                }
                canvas.translate(getPaddingLeft(), getPaddingTop());
                if (drawableWidth > drawableHeight) {
                    canvas.translate((drawableWidth - drawableHeight) * .5f, 0);
                } else {
                    canvas.translate(0, (drawableHeight - drawableWidth) * .5f);
                }
                if (isInEditMode()) {
                    float cr = Math.min(drawableWidth, drawableHeight) * .5f;
                    canvas.drawCircle(cr, cr, cr, mPaint);
                    return;
                }
                canvas.translate(d, mOffsetY + d);
                for (int i = 0; i < drawables.size(); i++) {
                    DrawableInfo drawable = drawables.get(i);
                    final int savedLayer = canvas.saveLayer(0, 0, mWidth - d, mHeight - d,
                            null, Canvas.ALL_SAVE_FLAG);
                    drawable.mDrawable.draw(canvas);
                    canvas.drawPath(drawable.mMaskPath, paint);
                    if (drawable.mHasGap && mGap > 0f) {
                        canvas.drawCircle(drawable.mGapCenterX, drawable.mGapCenterY, gapRadius, paint);
                    }
                    canvas.restoreToCount(savedLayer);
                }
                break;
            case left:
                canvas.translate(0, mPadding);
                //绘制圆角矩形
                //float[] array = {leftTopRound, leftTopRound, rightTopRound, rightTopRound, rightBottomRound, rightBottomRound, leftBottomRound, leftBottomRound};
                float[] arrayL = {0, 0, r, r, r, r, 0, 0};
                mPath.addRoundRect(new RectF(0f, 0, (float) mWidth, (float) mHeight), arrayL, Path.Direction.CW);
                mPaint.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, Color.GRAY);
                canvas.drawPath(mPath, mPaint);
                mPaint.clearShadowLayer();

                //绘制关闭的小叉
                Rect mSrcRectL = new Rect(0, 0, mCloseImageSize, mCloseImageSize);
                int leftL = (closeWidth - mCloseImageSize) / 2;
                int topL = mHeight / 2 - mCloseImageSize / 2;
                Rect mDestRectL = new Rect(leftL, topL, leftL + mCloseImageSize, topL + mCloseImageSize);
                canvas.drawBitmap(mBitmap, mSrcRectL, mDestRectL, mPaint);

                //绘制图片组合
                if (!isInEditMode() && (mContentSize <= 0 || N <= 0)) {
                    return;
                }
                canvas.translate(getPaddingLeft(), getPaddingTop());
                if (drawableWidth > drawableHeight) {
                    canvas.translate((drawableWidth - drawableHeight) * .5f, 0);
                } else {
                    canvas.translate(0, (drawableHeight - drawableWidth) * .5f);
                }
                if (isInEditMode()) {
                    float cr = Math.min(drawableWidth, drawableHeight) * .5f;
                    canvas.drawCircle(cr, cr, cr, mPaint);
                    return;
                }
                canvas.translate(d + closeWidth, mOffsetY + d);
                for (int i = 0; i < drawables.size(); i++) {
                    DrawableInfo drawable = drawables.get(i);
                    final int savedLayer = canvas.saveLayer(0, 0, mWidth - d, mHeight - d,
                            null, Canvas.ALL_SAVE_FLAG);
                    drawable.mDrawable.draw(canvas);
                    canvas.drawPath(drawable.mMaskPath, paint);
                    if (drawable.mHasGap && mGap > 0f) {
                        canvas.drawCircle(drawable.mGapCenterX, drawable.mGapCenterY, gapRadius, paint);
                    }
                    canvas.restoreToCount(savedLayer);
                }
                break;
            case move:
                if (mPartDirection == FloatingView.Direction.left) {
//                    mPaint.setColor(Color.TRANSPARENT);
//                    canvas.drawRect(0, 0, closeWidth, mHeight, mPaint);
                    canvas.translate(closeWidth, mPadding);
                } else {
                    canvas.translate(mPadding, mPadding);
                }
                mPaint.setColor(Color.WHITE);
                //绘制圆角矩形(长宽相同，圆角是长宽的一半，显示成圆形效果)
                //float[] array = {leftTopRound, leftTopRound, rightTopRound, rightTopRound, rightBottomRound, rightBottomRound, leftBottomRound, leftBottomRound};
                float[] arrayM = {r, r, r, r, r, r, r, r};
                mPath.addRoundRect(new RectF(0, 0, (float) mHeight, (float) mHeight), arrayM, Path.Direction.CW);
                mPaint.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, Color.GRAY);
                canvas.drawPath(mPath, mPaint);
                mPaint.clearShadowLayer();

                //绘制图片组合
                if (!isInEditMode() && (mContentSize <= 0 || N <= 0)) {
                    return;
                }
                canvas.translate(getPaddingLeft(), getPaddingTop());
                if (drawableWidth > drawableHeight) {
                    canvas.translate((drawableWidth - drawableHeight) * .5f, 0);
                } else {
                    canvas.translate(0, (drawableHeight - drawableWidth) * .5f);
                }
                if (isInEditMode()) {
                    canvas.drawCircle(r, r, r, mPaint);
                    return;
                }
                canvas.translate(d, mOffsetY + d);
                for (int i = 0; i < drawables.size(); i++) {
                    DrawableInfo drawable = drawables.get(i);
                    final int savedLayer = canvas.saveLayer(0, 0, mWidth - d, mHeight - d,
                            null, Canvas.ALL_SAVE_FLAG);
                    drawable.mDrawable.draw(canvas);
                    canvas.drawPath(drawable.mMaskPath, paint);
                    if (drawable.mHasGap && mGap > 0f) {
                        canvas.drawCircle(drawable.mGapCenterX, drawable.mGapCenterY, gapRadius, paint);
                    }
                    canvas.restoreToCount(savedLayer);
                }
                break;
        }
    }

    private void updateViewPosition(MotionEvent event) {
        // 限制不可超出屏幕宽度
        float desX = mOriginalX + event.getRawX() - mOriginalRawX;
        if (desX <= 0) {
            desX = 0;
        }
        if (desX > mWidthPixels) {
            desX = mWidthPixels;
        }
        setX(desX);

        // 限制不可超出屏幕高度
        float desY = mOriginalY + event.getRawY() - mOriginalRawY;
        if (desY <= MARGIN_EDGE_TOP) {
            desY = MARGIN_EDGE_TOP;
        }
        if (desY > mHeightPixels - getHeight() - Func.dp2px(mContext, MARGIN_EDGE_BOTTOM)) {
            desY = mHeightPixels - getHeight() - Func.dp2px(mContext, MARGIN_EDGE_BOTTOM);
        }
        setY(desY);
    }


    /**
     * 计算宽高
     */
    private int measureSize(int defaultSize, int measureSpec) {
        int result = defaultSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        //UNSPECIFIED	父容器没有对当前View有任何限制，当前View可以任意取尺寸
        //EXACTLY	当前的尺寸就是当前View应该取的尺寸
        //AT_MOST	当前尺寸是当前View能取的最大尺寸
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize);
        }
        return result;
    }

    /**
     * 判定所处方向
     */
    private void handleDirection(int x, int y) {
        if (x > (mWidthPixels / 2)) {
            mDirection = FloatingView.Direction.right;
            mMoveAnimator.start(mWidthPixels - mDefaultWidth, getY());
        } else {
            mDirection = FloatingView.Direction.left;
            mMoveAnimator.start(0, getY());
        }
        mPartDirection = mDirection;

    }


    /**
     * 调整悬浮窗位置
     * 根据提供坐标自动判断粘边
     */
    public void updateViewLayout(int x, int y) {
        handleDirection(x, y);
        invalidate();
    }


    /**
     * 方向
     */
    public enum Direction {
        /**
         * 左、右、移动
         */
        left,
        right,
        move
    }

    public void setFloatClickListener(FloatClickListener magnetViewListener) {
        this.mMagnetViewListener = magnetViewListener;
    }

    /**
     * 是否为点击事件
     */
    private boolean isOnClickEvent() {
        int scaledTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();// - 10;
        return Math.abs(xDownInScreen - xUpScreen) <= scaledTouchSlop
                && Math.abs(yDownInScreen - yUpScreen) <= scaledTouchSlop;
    }

    protected void dealClickEvent() {
        if (mMagnetViewListener != null) {
            mMagnetViewListener.onClick(this);
        }
    }

    private void initForEditMode() {
        if (!isInEditMode()) return;

        mImageGroupPaint.setXfermode(null);
        mImageGroupPaint.setColor(0xff0577fc);
    }

    protected boolean isNearestLeft() {
        int middle = mWidthPixels / 2;
        return getX() < middle;
    }

    /**
     * 设置图像间隙宽度
     *
     * @param gap the gap
     */
    public void setGap(@FloatRange(from = 0.f, to = 1.f) float gap) {
        gap = Math.max(0f, Math.min(gap, 1f));
        if (mGap != gap) {
            mGap = gap;
            invalidate();
        }
    }

    /**
     * 返回图像间隙宽度
     *
     * @return the gap
     */
    @FloatRange(from = 0.f, to = 1.f)
    public float getGap() {
        return mGap;
    }

    /**
     * @return drawable数量
     */
    @IntRange(from = 0, to = MAX_DRAWABLE_COUNT)
    public int getNumberOfDrawables() {
        return mDrawables.size();
    }

    /**
     * @return drawable的大小（高等于宽）
     */
    public int getDrawableSize() {
        return Math.round(mSteinerCircleRadius * 2);
    }

    /**
     * Drawable填充类型
     */
    public enum FitType {
        FIT,
        CENTER,
        START,
        END
    }

    /**
     * 设置Drawable填充类型
     *
     * @param fitType Drawable填充类型
     * @see FloatingView.FitType
     */
    public void setDrawableFitType(@NonNull FloatingView.FitType fitType) {
        //noinspection ConstantConditions
        if (fitType == null) {
            throw new NullPointerException();
        }
        if (mFitType != fitType) {
            mFitType = fitType;
            for (DrawableInfo drawableInfo : mDrawables) {
                updateDrawableBounds(drawableInfo);
            }
            invalidate();
        }
    }

    /**
     * @return Drawable填充类型
     */
    @NonNull
    public FloatingView.FitType getFitType() {
        return mFitType;
    }

    /**
     * 通过ID获取对应的drawable.
     *
     * @param id the id.
     * @return the drawable.
     */
    @Nullable
    public Drawable findDrawableById(int id) {
        for (DrawableInfo drawable : mDrawables) {
            if (drawable.mId == id) {
                return drawable.mDrawable;
            }
        }

        return null;
    }

    /**
     * 通过索引获取对应的drawable.
     *
     * @param index 索引
     * @return the drawable.
     */
    @NonNull
    public Drawable getDrawableAt(int index) {
        return mDrawables.get(index).mDrawable;
    }

    @Nullable
    private DrawableInfo findAvatarDrawableById(int id) {
        if (id != NO_ID) {
            for (DrawableInfo drawable : mDrawables) {
                if (drawable.mId == id) {
                    return drawable;
                }
            }
        }

        return null;
    }

    private boolean hasSameDrawable(Drawable drawable) {
        List<DrawableInfo> drawables = this.mDrawables;
        for (int i = 0; i < drawables.size(); i++) {
            if (drawables.get(i).mDrawable == drawable) {
                return true;
            }
        }

        return false;
    }

    /**
     * 添加drawable.
     *
     * @param drawable the drawable.
     * @return <code>true</code> - 如果添加成功， <code>false</code> - 其他
     * @see #addDrawable(int, Drawable)
     */
    public boolean addDrawable(@NonNull Drawable drawable) {
        return addDrawable(NO_ID, drawable);
    }

    /**
     * 添加drawable, 如果id已经存在, drawable将会被替换
     *
     * @param id       the drawable id.
     * @param drawable the drawable.
     * @return <code>true</code> - 如果添加成功， <code>false</code> - 其他
     */
    public boolean addDrawable(int id, @NonNull Drawable drawable) {
        DrawableInfo old = findAvatarDrawableById(id);
        if (old != null) {
            Drawable d = old.mDrawable;
            old.mDrawable = drawable;
            if (!hasSameDrawable(d)) {
                cleanDrawable(d);
            }
            updateDrawableBounds(old);
        } else {
            if (getNumberOfDrawables() >= MAX_DRAWABLE_COUNT) {
                return false;
            }
            mDrawables.add(crateAvatarDrawable(id, drawable));
            layoutDrawables();
        }
        drawable.setCallback(this);
        drawable.setVisible(getWindowVisibility() == VISIBLE && isShown(), true);
        if (drawable.isStateful()) {
            drawable.setState(getDrawableState());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawable.setLayoutDirection(getLayoutDirection());
        }
        invalidate();
        return true;
    }

    private DrawableInfo crateAvatarDrawable(int id, Drawable drawable) {
        DrawableInfo avatar = new DrawableInfo();
        avatar.mId = id;
        avatar.mDrawable = drawable;
        return avatar;
    }

    /**
     * 移除drawable.
     *
     * @param drawable the drawable.
     * @see #removeDrawableAt(int)
     * @see #removeDrawableById(int)
     */
    public void removeDrawable(@NonNull Drawable drawable) {
        List<DrawableInfo> drawables = this.mDrawables;
        for (int i = drawables.size() - 1; i >= 0; i--) {
            if (drawables.get(i).mDrawable == drawable) {
                removeDrawableAt(i);
            }
        }
    }

    /**
     * 通过id移除drawable.
     *
     * @param id the id.
     * @return 被移除的drawable，<code>null</code> - 如果id不存在。
     * @see #removeDrawableAt(int)
     * @see #removeDrawable(Drawable)
     */
    @Nullable
    public Drawable removeDrawableById(int id) {
        List<DrawableInfo> drawables = this.mDrawables;
        for (int i = 0; i < drawables.size(); i++) {
            if (drawables.get(i).mId == id) {
                return removeDrawableAt(i);
            }
        }
        return null;
    }

    /**
     * 通过索引移除drawable.
     *
     * @param index 索引
     * @return 被移除的drawable
     * @see #getNumberOfDrawables()
     * @see #removeDrawable(Drawable)
     * @see #removeDrawableById(int)
     */
    public Drawable removeDrawableAt(int index) {
        if (index > mDrawables.size() - 1) {
            return null;
        }
        DrawableInfo drawable = mDrawables.remove(index);
        if (!hasSameDrawable(drawable.mDrawable)) {
            cleanDrawable(drawable.mDrawable);
        }
        layoutDrawables();
        return drawable.mDrawable;
    }

    /**
     * 移除所有的drawable.
     */
    public void clearDrawable() {
        if (!mDrawables.isEmpty()) {
            for (DrawableInfo drawable : mDrawables) {
                cleanDrawable(drawable.mDrawable);
            }
            mDrawables.clear();
            layoutDrawables();
        }
    }

    private void cleanDrawable(Drawable drawable) {
        drawable.setCallback(null);
        unscheduleDrawable(drawable);
    }

    /**
     * 画图片
     */
    private void layoutDrawables() {
        mSteinerCircleRadius = 0;
        mOffsetY = 0;
        final List<DrawableInfo> drawables = mDrawables;
        //图片数量
        final int drawableSize = drawables.size();
        float center = mContentSize * .5f;
        if (mContentSize > 0 && drawableSize > 0) {
            // 图像圆的半径。
            final float r;
            if (drawableSize == 1) {
                r = mContentSize * .5f;
            } else if (drawableSize == 2) {
                r = (float) (mContentSize / (2 + 2 * Math.sin(Math.PI / 4)));
            } else if (drawableSize == 3) {
                r = (float) (mContentSize / (2 * (2 * Math.sin(((drawableSize - 2) * Math.PI)
                        / (2 * drawableSize)) + 1)));
                final double sinN = Math.sin(Math.PI / drawableSize);
                // 以所有图像圆为内切圆的圆的半径
                final float R = (float) (r * ((sinN + 1) / sinN));
                mOffsetY = (float)
                        ((mContentSize - R - r * (1 + 1 / Math.tan(Math.PI / drawableSize))) / 2f);
            } else if (drawableSize == 4) {
                r = mContentSize / 3.5f;
            } else {
                r = mContentSize / 4.5f;
                // 以所有图像圆为内切圆的圆的半径
//                final float R = (float) (r * ((sinN + 1) / sinN));
                mOffsetY = 0;
            }
            mSteinerCircleRadius = r;
            final float startX, startY;
            if (drawableSize % 2 == 0) {//偶数个圆
                startX = startY = r;
            } else {//基数个圆
                startX = center;
                startY = r;
            }
            final Matrix matrix = mLayoutMatrix;
            final float[] pointsTemp = this.mPointsTemp;
            matrix.reset();
            for (int i = 0; i < drawables.size(); i++) {
                DrawableInfo drawable = drawables.get(i);
                drawable.reset();
                drawable.mHasGap = i > 0;
                if (drawable.mHasGap) {
                    drawable.mGapCenterX = pointsTemp[0];
                    drawable.mGapCenterY = pointsTemp[1];
                }
                pointsTemp[0] = startX;
                pointsTemp[1] = startY;
                if (i > 0) {
                    // 以上一个圆的圆心旋转计算得出当前圆的圆位置
                    matrix.postRotate(360.f / drawableSize, center, center + mOffsetY);
                    matrix.mapPoints(pointsTemp);
                }
                drawable.mCenterX = pointsTemp[0];
                drawable.mCenterY = pointsTemp[1];
                updateDrawableBounds(drawable);
                drawable.mMaskPath.addCircle(drawable.mCenterX, drawable.mCenterY, r,
                        Path.Direction.CW);
                drawable.mMaskPath.setFillType(Path.FillType.INVERSE_WINDING);
            }
            if (drawableSize > 2) {
                DrawableInfo first = drawables.get(0);
                DrawableInfo last = drawables.get(drawableSize - 1);
                first.mHasGap = true;
                first.mGapCenterX = last.mCenterX;
                first.mGapCenterY = last.mCenterY;
            }
        }
        invalidate();
    }

    private void updateDrawableBounds(DrawableInfo drawableInfo) {
        final Drawable drawable = drawableInfo.mDrawable;
        final float radius = mSteinerCircleRadius;
        if (radius <= 0) {
            drawable.setBounds(0, 0, 0, 0);
            return;
        }
        final int dWidth = drawable.getIntrinsicWidth();
        final int dHeight = drawable.getIntrinsicHeight();
        final RectF bounds = mTempBounds;
        bounds.setEmpty();
        if (dWidth <= 0 || dHeight <= 0 || dWidth == dHeight || FloatingView.FitType.FIT == mFitType) {
            bounds.inset(-radius, -radius);
        } else {
            float scale;
            if (dWidth > dHeight) {
                scale = radius / (float) dHeight;
            } else {
                scale = radius / (float) dWidth;
            }
            bounds.inset(-dWidth * scale, -dHeight * scale);

            if (FloatingView.FitType.START == mFitType || FloatingView.FitType.END == mFitType) {
                int dir = FloatingView.FitType.START == mFitType ? 1 : -1;
                bounds.offset((bounds.width() * 0.5f - radius) * dir,
                        (bounds.height() * 0.5f - radius) * dir);
            }
        }
        bounds.offset(drawableInfo.mCenterX, drawableInfo.mCenterY);
        drawable.setBounds((int) bounds.left, (int) bounds.top,
                Math.round(bounds.right), Math.round(bounds.bottom));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        layoutDrawables();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        updateVisible();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateVisible();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateVisible();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateVisible();
    }

    private void updateVisible() {
        boolean isVisible = getWindowVisibility() == VISIBLE && isShown();
        for (DrawableInfo drawable : mDrawables) {
            drawable.mDrawable.setVisible(isVisible, false);
        }
        if (isVisible) {
            layoutDrawables();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for (DrawableInfo drawable : mDrawables) {
            drawable.mDrawable.setVisible(false, false);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        boolean invalidate = false;
        for (DrawableInfo drawable : mDrawables) {
            Drawable d = drawable.mDrawable;
            if (d.isStateful() && d.setState(getDrawableState())) {
                invalidate = true;
            }
        }

        if (invalidate) {
            invalidate();
        }
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        for (DrawableInfo drawable : mDrawables) {
            drawable.mDrawable.jumpToCurrentState();
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable drawable) {
        return hasSameDrawable(drawable) || super.verifyDrawable(drawable);
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        if (hasSameDrawable(drawable)) {
            invalidate();
        } else {
            super.invalidateDrawable(drawable);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return FloatingView.class.getName();
    }

    protected class MoveAnimator implements Runnable {
        private Handler handler = new Handler(Looper.getMainLooper());
        private float destinationX;
        private float destinationY;
        private long startingTime;

        void start(float x, float y) {
            this.destinationX = x;
            this.destinationY = y;
            startingTime = System.currentTimeMillis();
            handler.post(this);
        }

        @Override
        public void run() {
            if (getRootView() == null || getRootView().getParent() == null) {
                return;
            }
            float progress = Math.min(1, (System.currentTimeMillis() - startingTime) / 400f);
            float deltaX = (destinationX - getX()) * progress;
            float deltaY = (destinationY - getY()) * progress;
            move(deltaX, deltaY);
            if (progress < 1) {
                handler.post(this);
            }
        }

        private void stop() {
            handler.removeCallbacks(this);
        }
    }

    private void move(float deltaX, float deltaY) {
        setX(getX() + deltaX);
        setY(getY() + deltaY);
    }
}
