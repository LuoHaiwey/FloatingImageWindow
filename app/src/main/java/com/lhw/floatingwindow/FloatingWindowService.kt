package com.lhw.floatingwindow

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lhw.floatingwindow.widgets.FloatingWindow


/**
 * ****************************************************************
 * 文件名称 : com.ziwenl.floatingwindowdemo.widgets.FloatingWindow
 * 作    者 : lhw
 * 创建时间 : 2021/06/28 14:40
 * 文件描述 : 悬浮窗管理服务 -- 一般在此处理业务逻辑
 * 版权声明 : Copyright (C) 2015-2018 杭州中焯信息技术股份有限公司
 * 修改历史 : 2021/06/28  1.00 初始版本
 * ****************************************************************
 */
class FloatingWindowService : Service() {

    companion object {
        private var mServiceVoice: FloatingWindowService? = null
        const val ACTION_SHOW_FLOATING = "action_show_floating"
        const val ACTION_DISMISS_FLOATING = "action_dismiss_floating"
        var isStart = false

        fun stopSelf() {
            mServiceVoice?.stopSelf()
            mServiceVoice = null
        }
    }

    private var mFloatingWindow: FloatingWindow? = null

    /**
     * 监听本地广播显示或隐藏悬浮窗
     */
    private var mLocalBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ACTION_SHOW_FLOATING == intent?.action) {
                mFloatingWindow?.show()
            } else if (ACTION_DISMISS_FLOATING == intent?.action) {
                mFloatingWindow?.dismiss()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        mServiceVoice = this
        isStart = true
        //初始化悬浮View
        mFloatingWindow = FloatingWindow(this)
        mFloatingWindow?.addDrawable(resources.getDrawable(R.drawable.image_1))
        mFloatingWindow?.addDrawable(resources.getDrawable(R.drawable.image_2))
        mFloatingWindow?.addDrawable(resources.getDrawable(R.drawable.image_3))
        mFloatingWindow?.addDrawable(resources.getDrawable(R.drawable.image_4))
        mFloatingWindow?.addDrawable(resources.getDrawable(R.drawable.image_5))
        //注册监听本地广播
        val intentFilter = IntentFilter(ACTION_SHOW_FLOATING)
        intentFilter.addAction(ACTION_DISMISS_FLOATING)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mLocalBroadcastReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //显示悬浮窗
        mFloatingWindow?.show()
        mFloatingWindow?.setFloatClickListener {
            val voiceActivityIntent = Intent(this@FloatingWindowService, MainActivity::class.java)
//            voiceActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER)
//            voiceActivityIntent.action = Intent.ACTION_MAIN
//            voiceActivityIntent.flags = FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            startActivity(voiceActivityIntent)
            mFloatingWindow?.dismiss()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mFloatingWindow?.dismiss()
        mFloatingWindow = null
        isStart = false
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadcastReceiver)
        super.onDestroy()
    }
}