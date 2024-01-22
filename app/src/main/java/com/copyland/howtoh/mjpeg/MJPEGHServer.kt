package com.copyland.howtoh.mjpeg

import android.content.Context
import android.util.Log
import com.copyland.howtoh.http.HttpServer
import java.net.Socket


class MJPEGHServer(
    private val widthPixel: Int,
    private val heightPixel: Int,
    port: Int,
    context: Context
) : HttpServer(port, context) {
    private val prefix :String = "webroot"
    override fun handlePath(sock: Socket, path: String) {
        Log.i("SM", "HTTP req: $path")
        if (path == "/"){
            val handler = StaticResourceHandler(sock,context)
            handler.filePos = "$prefix/index.html"
            handler.start()
        }else if (path.endsWith(".js", true)
            || path.endsWith(".html", true)
            || path.endsWith(".css", true)
            || path.endsWith(".png", true)){
            val handler = StaticResourceHandler(sock,context)
            handler.filePos = prefix + path
            handler.start()
        } else if (path.endsWith("/tesla.mjpeg")){
            MJPEGHandler(sock, widthPixel, heightPixel, context).start()
        }else {
            ErrorHandler(sock, context).start()
        }
    }


    open fun stopService(){
        this.interrupt()
        mServerSock.close()
    }
}
