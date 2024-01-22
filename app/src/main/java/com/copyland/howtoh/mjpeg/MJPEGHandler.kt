package com.copyland.howtoh.mjpeg

import android.content.Context
import android.util.Log
import com.copyland.howtoh.http.HttpHandler
import com.copyland.howtoh.screencapturer.ScreenCapture
import java.net.Socket


class MJPEGHandler(sock: Socket?, widthPixel: Int, heightPixel: Int, context: Context) : HttpHandler(
    sock!!, context) {
    private val mFrameBuffer: ByteArray
    private val mColors: IntArray
    private val mWidth: Int
    private val mHeight: Int


    init {
        mWidth = widthPixel
        mHeight = heightPixel
        mFrameBuffer = ByteArray(mWidth * mHeight * 4)
        mColors = IntArray(mWidth * mHeight)
    }

    override fun run() {
        try {
            Log.i("SM", "Send buffer size: " + sock.sendBufferSize)
            val os = sock.getOutputStream()
            val boundary = "screenFrame"
            val htmlHeader = "HTTP/1.1 200 OK\r\n" +
                    "Date: " + SERVER_TIME + "\r\n" +
                    "Server: " + SERVER_NAME + "\r\n" +
                    "Content-Type: multipart/x-mixed-replace; boundary=--" +
                    boundary + "\r\n" +
                    "Cache-Control: no-cache, private\r\n" +
                    "Pragma: no-cache\r\n" +
                    "Max-Age: 0\r\n" +
                    "Expires: 0\r\n" +
                    "Connection: keep-alive\r\n\r\n"
            os.write(
                   htmlHeader.toByteArray()
            )
            val obj = Object()


            while (!isInterrupted) {
                val boss = ScreenCapture.getInstance().imageQueue.take()
                val imageData = boss.toByteArray()
                val htmlContent = "--" + boundary + "\r\n" +
                        "Content-type: image/jpeg\r\n" +
                        "Content-Length: " + imageData.size +
                        "\r\n\r\n"
                os.write( htmlContent.toByteArray())
                os.write(imageData)
                os.write("\r\n".toByteArray())
                os.flush()
                boss.close()
                synchronized(obj){
                    try {
                        obj.wait(36)
                    }catch (e: InterruptedException){
                        Log.e("SM", "MJPEG InterruptedException", e)
                    }
                }
            }
            sock.close()
        } catch (e: Exception) {
            Log.e("SM", "MJPEG stream error", e)
        }
    }

    @Suppress("unused")
    private inner class Timer {
        private val mTimers: MutableMap<String, Long>

        init {
            mTimers = HashMap()
        }

        fun reset(name: String) {
            mTimers[name] = System.currentTimeMillis()
        }

        fun stop(name: String) {
            if (mTimers.containsKey(name)) {
                val curr = System.currentTimeMillis()
                Log.i(
                    "SM", name + " timing is " + (curr - mTimers[name]!!)
                            + "ms"
                )
            }
        }
    }

    @Suppress("unused")
    private inner class Speed {
        private var mSpeedStart: Long = 0

        init {
            reset()
        }

        fun reset() {
            mSpeedStart = System.currentTimeMillis()
        }

        fun print(sent: Int) {
            val kb = sent / 1024.0f
            val dt = (System.currentTimeMillis() - mSpeedStart) / 1000.0f
            if (dt != 0f) {
                Log.i("SM", "Speed " + kb / dt + "KB/s")
            }
        }
    }
}
