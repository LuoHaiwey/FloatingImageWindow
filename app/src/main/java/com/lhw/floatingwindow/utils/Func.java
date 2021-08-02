package com.lhw.floatingwindow.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;

/**
 * ****************************************************************
 * 文件名称 : com.ziwenl.floatingwindowdemo.utils.Func
 * 作    者 : lhw
 * 创建时间 : 2021/06/28 17:46
 * 文件描述 : java类作用描述
 * 版权声明 : Copyright (C) 2015-2018 杭州中焯信息技术股份有限公司
 * 修改历史 : 2021/06/28  1.00 初始版本
 * ****************************************************************
 */
public class Func {
    public static Bitmap drawable2Bitmap(Drawable drawable, int size) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        Matrix matrix=new Matrix();
        float scale=size*1f/Math.max(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        matrix.postScale(scale,scale);
        return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
    }

    public static int dp2px(Context context,float value) {
        float scale = context.getResources().getDisplayMetrics().densityDpi;
        return (int) (value * (scale / 160) + 0.5f);
    }
}
