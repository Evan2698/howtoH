package com.copyland.howtoh.http

import android.util.Log

class MessagePump {
    companion object{

        @Volatile
        private  var pump: MessagePump? = null
        fun getInstance():MessagePump{
            if (pump == null){
                synchronized(this){
                    if (pump == null){
                        pump = MessagePump()
                    }
                }
            }
            return pump!!
        }

        private val TAG=MessagePump::class.java.simpleName
    }

    private var handler : ActionHandler? = null

    fun setActionHandler(handler: ActionHandler){
        this.handler = handler
    }
    fun dispatchMessage(msg: String){
        if (msg.isEmpty()) return
        Log.d(TAG, "byteArray is ${msg.toString()}")
        val xy = msg.split(",")
        if(xy.isEmpty() || xy.size < 2) return
        val x = xy[0].toInt()
        val y = xy[1].toInt()
        if (this.handler != null){
            this.handler!!.click(x, y)
            Log.d(TAG,  "coordinate x=$x, y=$y" )
        }
    }
}