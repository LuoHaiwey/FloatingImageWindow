package com.lhw.floatingwindow.widgets;

import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * ****************************************************************
 * 文件名称 : com.ziwenl.floatingwindowdemo.widgets.DrawableInfo
 * 作    者 : lhw
 * 创建时间 : 2021/06/28 16:29
 * 文件描述 : 图片信息类
 * 版权声明 : Copyright (C) 2015-2018 杭州中焯信息技术股份有限公司
 * 修改历史 : 2021/06/28  1.00 初始版本
 * ****************************************************************
 */
public class DrawableInfo {
    int mId = View.NO_ID;
    Drawable mDrawable;
    float mCenterX;
    float mCenterY;
    float mGapCenterX;
    float mGapCenterY;
    boolean mHasGap;
    final Path mMaskPath = new Path();

    void reset() {
        mCenterX = 0;
        mCenterY = 0;
        mGapCenterX = 0;
        mGapCenterY = 0;
        mHasGap = false;
        mMaskPath.reset();
    }
}
