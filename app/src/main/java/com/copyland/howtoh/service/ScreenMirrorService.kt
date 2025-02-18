package com.copyland.howtoh.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.copyland.howtoh.MainActivity
import com.copyland.howtoh.R
import com.copyland.howtoh.http.HttpMiniServer
import com.copyland.howtoh.screencapturer.ScreenCapture


class ScreenMirrorService : Service() {

    companion object {
        private val TAG:String = ScreenMirrorService::class.java.simpleName
        const val SERVICE_STOP_ACTION = "stop-action"
        const val SERVICE_START_ACTION = "start-action"
        const val SERVICE_STATUS = "SERVICE_STATUS"
        const val SERVICE_STATUS_ACTION = "SERVICE_STATUS_ACTION"

        private const val SERVICE_ID = 102
        private const val NOTIFICATION_CHANNEL_ID = "ScreenMirrorServiceChannel"
        private const val NOTIFICATION_CHANNEL_NAME = "Screen mirror notification channel"
        private const val NOTIFICATION_TITLE = "Screen Mirror is running"
        private const val NOTIFICATION_CONTENT = "Tap to stop"

        var IsServiceRunning = false
            private set

        private const val PORT:Int = 8080
    }

    private var isRunning : Boolean = false
    private var httpServer :HttpMiniServer? = null


    inner class MirrorServiceBinder : Binder() {
        val service: ScreenMirrorService
            get() = this@ScreenMirrorService
    }

    private val iBinder: IBinder = MirrorServiceBinder()

    override fun onBind(intent: Intent?): IBinder {
        return iBinder
    }


    override fun onCreate() {
        super.onCreate()
        IsServiceRunning = true
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            SERVICE_START_ACTION -> start()
            SERVICE_STOP_ACTION -> stop()
            else -> onDefault()
        }

        Log.d(TAG, "onStartCommand")


        return START_STICKY
    }

    private fun onDefault() {

    }


    @SuppressLint("LaunchActivityFromNotification")
    private fun start() {

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.action = Intent.ACTION_MAIN
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pendingIntent = PendingIntent.getService(
            this, 0,
            notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val channelId =
            createNotificationChannel()
        val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(
            this,
            channelId
        )
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_CONTENT)
            .setSmallIcon(R.drawable.screen_mirror)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        startForeground(SERVICE_ID, notification)
        Log.d(TAG, "services start----->")
        this.sendServiceStatus(1)


    }

    private fun createNotificationChannel(): String {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager!!.createNotificationChannel(channel)
        return NOTIFICATION_CHANNEL_ID
    }

    private fun stop() {
        stopService()
        this.sendServiceStatus(2)
    }

    fun startService(intent: Intent, context: Context, landscape:Boolean){
        val cm = ScreenMetricsCompat.getScreenSize(context)
        val widthPixel = cm.width
        val heightPixel = cm.height
        RatioHolder.getInstance().screenHeight = heightPixel.toDouble()
        RatioHolder.getInstance().screenWidth = widthPixel.toDouble()
        val ratio = when (widthPixel) {
            in 0..480 -> 1
            in 481..720 -> 2
            in 721..1080 -> 3
            in 1081..2600 -> 4
            else -> 6
        }
        var w = widthPixel / ratio
        var h = heightPixel /ratio
        RatioHolder.getInstance().realWidth = w.toDouble()
        RatioHolder.getInstance().realHeight = h.toDouble()
        if (landscape){
            w = h.apply { h = w }
        }
        Log.d("SM", "startS x=${cm.width}, y=${cm.height}," +
                " X1=${w}, Y1=${h}")
        val k = ScreenCapture.getInstance()
        k.start(w, h, context, intent)
        httpServer = HttpMiniServer(PORT, k)
        httpServer?.start()
        isRunning = true
    }

    private fun stopService(){
        if (httpServer != null){
            httpServer?.stop()
        }
        ScreenCapture.getInstance().stop()
        isRunning = false
        IsServiceRunning = false
    }

    fun isServerRunning():Boolean{
        return isRunning
    }

    private fun sendServiceStatus(status: Int) {
        val intent = Intent()
        intent.action = SERVICE_STATUS_ACTION
        intent.putExtra(SERVICE_STATUS, status)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        this.sendServiceStatus(3)
        IsServiceRunning = false
    }

    object ScreenMetricsCompat {
        private val api: Api =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ApiLevel30()
            else Api()

        /**
         * Returns screen size in pixels.
         */
        fun getScreenSize(context: Context): Size = api.getScreenSize(context)

        @Suppress("DEPRECATION")
        private open class Api {
            open fun getScreenSize(context: Context): Size {
                val display = context.getSystemService(WindowManager::class.java).defaultDisplay
                val metrics = if (display != null) {
                    DisplayMetrics().also { display.getRealMetrics(it) }
                } else {
                    Resources.getSystem().displayMetrics
                }
                return Size(metrics.widthPixels, metrics.heightPixels)
            }
        }

        @RequiresApi(Build.VERSION_CODES.R)
        private class ApiLevel30 : Api() {
            override fun getScreenSize(context: Context): Size {
                val metrics: WindowMetrics = context.getSystemService(WindowManager::class.java).currentWindowMetrics
                return Size(metrics.bounds.width(), metrics.bounds.height())
            }
        }
    }
}