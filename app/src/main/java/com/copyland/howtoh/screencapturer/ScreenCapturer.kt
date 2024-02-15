package com.copyland.howtoh.screencapturer

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjection.Callback
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.copyland.howtoh.http.JPEGCache
import java.io.ByteArrayOutputStream
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue


class ScreenCapture private constructor(): JPEGCache {

    companion object {
        private var TAG: String = ScreenCapture::class.java.simpleName

        @Volatile
        private var instance: ScreenCapture? = null
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: ScreenCapture().also { instance = it }
            }
    }

    private var mediaProjection: MediaProjection? = null
    private var projectionManager: MediaProjectionManager?= null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    @Volatile
    private var interrupt: Boolean = false

    private var handler: Handler? = null
    private var looper: Looper?= null
    private var imageQueue: BlockingQueue<ByteArrayOutputStream> =
        LinkedBlockingQueue(2)

    private fun initFunc(context: Context?, intent: Intent?) {
        projectionManager =
            context?.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager

        mediaProjection = projectionManager?.getMediaProjection(RESULT_OK, intent!!)
        mediaProjection?.registerCallback(object : Callback() {

        }, null)
    }

    fun start(widthPixel: Int, heightPixel: Int,context: Context, intent: Intent ) {
        // reset the image queue for application restart.
        interrupt = false
        this.clear()
        initFunc(context, intent)
        startActionLoop(widthPixel, heightPixel)
    }

    private fun makeCapture(widthPixel: Int, heightPixel: Int) {
        imageReader = ImageReader.newInstance(widthPixel, heightPixel, PixelFormat.RGBA_8888, 1)
        imageReader?.setOnImageAvailableListener({
            captureAction(it)
        }, handler!!)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            widthPixel, heightPixel,
            Resources.getSystem().displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler!!
        )
    }

    private fun startActionLoop(widthPixel: Int, heightPixel: Int){
        Thread{
            Looper.prepare()
            looper = Looper.myLooper()!!
            handler = Handler(looper!!, object :Callback(), Handler.Callback {
                override fun handleMessage(msg: Message): Boolean {
                    Log.d(TAG, msg.toString())
                    return false
                }
            })
            makeCapture(widthPixel, heightPixel)
            Looper.loop()
            Log.d("SM", "Capture loop Exit~~~~~!!!!")
        }.start()
    }

    fun stop() {
        mediaProjection?.stop()
        virtualDisplay?.release()
        interrupt = true
        mediaProjection = null
        virtualDisplay = null
        looper?.quit()
        imageReader?.close()
        imageReader = null
        looper = null
        handler = null
    }

    private fun captureAction(reader: ImageReader) {
        val image: Image = reader.acquireLatestImage()
        processImage(image)
        image.close()
    }



    private fun processImage(image: Image) {
        try {
            val width = image.width
            val height = image.height
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width
            val widthPixel = width + rowPadding / pixelStride
            val bitmap = Bitmap.createBitmap(widthPixel, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            val newBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            bitmap.recycle()
            val boss = ByteArrayOutputStream()
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 80, boss)
            boss.flush()
            newBitmap.recycle()
            if (boss.size() != 0){
                imageQueue.put(boss)
            }else {
                Log.d("SM", "capture empty image array!!!------->")
            }


        } catch (e: Exception) {
            Log.d(TAG, "process", e)
        }
    }

    override fun takeImageFromStream(): ByteArrayOutputStream {
        var temp = ByteArrayOutputStream()
        if (interrupt){
            return temp
        }

        try {
            temp = imageQueue.take()
        }catch (e: Exception){
            Log.d(TAG, "imageQueue take failed: ", e)
        }
        return temp
    }

    override fun clear() {
        imageQueue.clear()
    }

    override fun stopCapture() {
        imageQueue.put(ByteArrayOutputStream())
    }
}