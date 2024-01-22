package com.copyland.howtoh.mjpeg

import android.content.Context
import android.util.Log
import com.copyland.howtoh.http.HttpHandler
import java.net.Socket


class ErrorHandler(sock: Socket?, context: Context) : HttpHandler(
    sock!!, context) {
    override fun run() {
        try {
            var htmlContent = "HTTP/1.1 404 Not Found\r\n" +
                    "Date: " + SERVER_TIME + "\r\n" +
                    "Server: " + SERVER_NAME + "\r\n" +
                    "Content-Type: text/plain;charset=utf-8\r\n" +
                    "Connection: close\r\n\r\n" +
                    "Not Found"
            val os = sock.getOutputStream()
            os.write(
                htmlContent.toByteArray()
            )
            sock.close()
        } catch (e: Exception) {
            Log.e("SM", "Home page handler error", e)
        }
    }
}
