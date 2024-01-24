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


class ScreenCapture private constructor(intent: Intent, context: Context): JPEGCache {

    companion object {
        private var TAG: String = ScreenCapture::class.java.simpleName
        private var intent: Intent? = null
        private var context: Context? = null



        @Volatile
        private var instance: ScreenCapture? = null
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: ScreenCapture(this.intent!!, this.context!!).also { instance = it }
            }

        fun builder(intent: Intent, context: Context): ScreenCapture {
            this.intent = intent
            this.context = context
            return getInstance()
        }
    }

    private val context: Context
    private val intent: Intent


    private var mediaProjection: MediaProjection? = null
    private lateinit var projectionManager: MediaProjectionManager
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var imageReader: ImageReader
    @Volatile
    private var interrupt: Boolean = false

    private lateinit var handler: Handler
    private lateinit var looper: Looper
    private var imageQueue: BlockingQueue<ByteArrayOutputStream> =
        LinkedBlockingQueue<ByteArrayOutputStream>(2)

    init {
        this.intent = intent
        this.context = context
        initFunc()
    }

    private fun initFunc() {
        projectionManager =
            this.context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager

        mediaProjection = projectionManager.getMediaProjection(RESULT_OK, this.intent);
        mediaProjection?.registerCallback(object : Callback() {
            override fun onStop() {
                super.onStop()
            }

            override fun onCapturedContentResize(width: Int, height: Int) {
                super.onCapturedContentResize(width, height)
            }

            override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                super.onCapturedContentVisibilityChanged(isVisible)
            }

        }, null)

    }

    fun start(widthPixel: Int, heightPixel: Int) {
        startActionLoop(widthPixel, heightPixel)
    }

    private fun makeCapture(widthPixel: Int, heightPixel: Int) {
        imageReader = ImageReader.newInstance(widthPixel, heightPixel, PixelFormat.RGBA_8888, 1)
        imageReader.setOnImageAvailableListener({
            captureAction(it)
        }, handler)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            widthPixel, heightPixel,
            Resources.getSystem().displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, handler
        )
    }

    private fun startActionLoop(widthPixel: Int, heightPixel: Int){
        Thread(Runnable{
            Looper.prepare()
            looper = Looper.myLooper()!!
            handler = Handler(looper, object :Callback(), Handler.Callback {
                override fun handleMessage(msg: Message): Boolean {
                    Log.d(TAG, msg.toString())
                    return false
                }
            })
            makeCapture(widthPixel, heightPixel)
            Looper.loop()
        }).start()
    }

    fun stop() {
        mediaProjection?.stop()
        virtualDisplay?.release()
        interrupt = true;
        mediaProjection = null
        virtualDisplay = null
        looper!!.quit()
        imageQueue.put(ByteArrayOutputStream())
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
            val bitmap = Bitmap.createBitmap(widthPixel, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            val newBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            bitmap.recycle()
            val boss = ByteArrayOutputStream()
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 70, boss)
            boss.flush()
            newBitmap.recycle()
            imageQueue.put(boss)

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


}