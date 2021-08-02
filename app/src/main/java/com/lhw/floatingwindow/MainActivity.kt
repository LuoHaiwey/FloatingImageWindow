package com.lhw.floatingwindow

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mFloatingView?.addDrawable(resources.getDrawable(R.drawable.image_1))
        mFloatingView?.addDrawable(resources.getDrawable(R.drawable.image_2))
        mFloatingView?.addDrawable(resources.getDrawable(R.drawable.image_3))
        mFloatingView?.addDrawable(resources.getDrawable(R.drawable.image_4))
        mFloatingView?.addDrawable(resources.getDrawable(R.drawable.image_5))
        mFloatingView?.setFloatClickListener {
            Toast.makeText(this, "点击", Toast.LENGTH_SHORT).show()
        }
        btn_show_inapp.setOnClickListener {
            mFloatingView?.visibility = View.VISIBLE
            dismissFloatingView()
        }

        btn_show_insystem.setOnClickListener {
            mFloatingView?.visibility = View.GONE
            //Android 6.0 以下无需获取权限，可直接展示悬浮窗
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //判断是否拥有悬浮窗权限，无则跳转悬浮窗权限授权页面
                if (Settings.canDrawOverlays(this)) {
                    showFloatingView()
                } else {
                    //跳转悬浮窗权限授权页面
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            } else {
                showFloatingView()
            }
        }
    }

    /**
     * 显示悬浮窗
     */
    private fun showFloatingView() {
        if (FloatingWindowService.isStart) {
            //通知显示悬浮窗
            LocalBroadcastManager.getInstance(this)
                .sendBroadcast(Intent(FloatingWindowService.ACTION_SHOW_FLOATING))
        } else {
            //启动悬浮窗管理服务
            startService(Intent(this, FloatingWindowService::class.java))
        }
    }

    //隐藏悬浮窗
    private fun dismissFloatingView() {
        if (FloatingWindowService.isStart) {
            LocalBroadcastManager.getInstance(this)
                .sendBroadcast(Intent(FloatingWindowService.ACTION_DISMISS_FLOATING))
        }
    }
}
