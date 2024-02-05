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
        try {
            Log.d(TAG, "byteArray is ${msg.toString()}")
            val xy = msg.split(",")
            if(xy.isEmpty() || xy.size < 3) return
            when(xy[0]){
                "M"->handleMouse(xy[1].toDouble(), xy[2].toDouble())
                "K"->handleKey(xy[1])
            }
        }catch (e:Exception){
            Log.d(TAG, "what", e)
        }
    }

    private fun handleMouse(x:Double, y:Double){
        if (this.handler != null){
            this.handler!!.click(x, y)
            Log.d(TAG,  "coordinate x=$x, y=$y" )
        }
    }

    private fun handleKey(key:String){
        if (this.handler != null){
            this.handler!!.key(key)
            Log.d(TAG,  "key click: $key" )
        }
    }
}