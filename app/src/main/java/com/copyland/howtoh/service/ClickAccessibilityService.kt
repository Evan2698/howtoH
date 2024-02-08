package com.copyland.howtoh.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.res.Resources
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.copyland.howtoh.http.ActionHandler
import com.copyland.howtoh.http.MessagePump


class ClickAccessibilityService: AccessibilityService(), ActionHandler {

    override fun onCreate() {
        super.onCreate()
        MessagePump.getInstance().setActionHandler(this)
    }
    companion object{
        private var TAG:String = ClickAccessibilityService::class.java.simpleName
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 处理辅助功能事件
    }

    override fun onInterrupt() {
        // 辅助功能中断时执行的操作
    }

    override fun click(x:Double, y:Double){
        val xFloat = x / RatioHolder.getInstance().realWidth *
                RatioHolder.getInstance().screenWidth
        val yFloat = y / RatioHolder.getInstance().realHeight *
                RatioHolder.getInstance().screenHeight

        val cm = Resources.getSystem().displayMetrics
        Log.d(TAG, "current X=$xFloat, Y=$yFloat, Screen X= ${cm.widthPixels}, Y=${cm.heightPixels}, dip=${cm.densityDpi}")

        val path = Path()
        path.moveTo(xFloat.toFloat(), yFloat.toFloat())
        val builder = GestureDescription.Builder()
        val gestureDescription = builder
            .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
            .build()
        val result = dispatchGesture(gestureDescription, object :GestureResultCallback(){
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "onCompleted: Click..........")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.d(TAG, "onCompleted: Cancel..........")
            }

        }, null)

        Log.i(TAG,"dispatch gesture: $result, x = $xFloat, y = $yFloat")
    }

    override fun key(value: String){
        when(value){
            "H"->performGlobalAction(GLOBAL_ACTION_HOME)
            "B"->performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }
}