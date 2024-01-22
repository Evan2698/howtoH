package com.copyland.howtoh.mjpeg
import android.content.Context
import android.util.Log
import com.copyland.howtoh.http.HttpHandler
import java.net.Socket


class HomeHandler(sock: Socket?,context: Context) : HttpHandler(sock!!,context) {
    companion object {
        private const val HTML =
            "<!DOCTYPE html> <html><div style=\"width:100%; text-align:center\"><img src=\"frame.mjpeg\" alt=\"screen\" /></div></html>"
    }

    override fun run() {
        Log.d("SM", "HomeHandler--------------->")

        var htmlContent = "HTTP/1.1 200 OK\r\n" +
                "Date: " + HttpHandler.Companion.SERVER_TIME + "\r\n"+
                "Server: " + SERVER_NAME + "\r\n" +
                "Content-Type: text/html;charset=utf-8\r\n" +
                "Connection: close\r\n\r\n"+HTML
        try {
            val os = sock.getOutputStream()
            os.write(
                htmlContent.toByteArray()
            )
            os.flush()
            sock.close()
        } catch (e: Exception) {
            Log.e("SM", "Home page handler error", e)
        }
    }


}
