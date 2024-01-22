package com.copyland.howtoh.mjpeg

import android.content.Context
import android.util.Log
import com.copyland.howtoh.http.HttpHandler
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.Socket


class StaticResourceHandler(sock: Socket?, context: Context) : HttpHandler(sock!!, context) {

    public var filePos:String = ""

    private var  htmlHeader:String = "HTTP/1.1 200 OK\r\n" +
            "Date: " + SERVER_TIME + "\r\n"+
            "Server: " + SERVER_NAME + "\r\n" +
            "Accept-Language: en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7\r\n" +
            "Content-Type: text/html;charset=utf-8\r\n" +
            "Cache-Control: no-cache \r\n"

    private var jsHeader:String = "HTTP/1.1 200 OK\r\n" +
            "Date: " + SERVER_TIME + "\r\n"+
            "Server: " + SERVER_NAME + "\r\n" +
            "Content-Type: application/javascript;charset=utf-8\r\n" +
            "Cache-Control: no-cache \r\n"

    private var cssHeader:String = "HTTP/1.1 200 OK\r\n" +
            "Date: " + SERVER_TIME + "\r\n"+
            "Server: " + SERVER_NAME + "\r\n" +
            "Content-Type: text/css;charset=utf-8\r\n" +
            "Cache-Control: no-cache \r\n"


    private val headerEndTag:String = "Connection: close\r\n\r\n"

    private val errorContent = "HTTP/1.1 404 Not Found\r\n" +
            "Date: " + SERVER_TIME + "\r\n" +
            "Server: " + SERVER_NAME + "\r\n" +
            "Content-Type: text/plain;charset=utf-8\r\n" +
            "Connection: close\r\n\r\n"


    private val pngHeader = "HTTP/1.1 200 OK\r\n" +
            "Date: " + SERVER_TIME + "\r\n"+
            "Server: " + SERVER_NAME + "\r\n" +
            "Content-Type: image/png\r\n" +
            "Cache-Control: no-cache \r\n" +
            "Content-Length: "

    override fun run() {
        var content = ByteArrayOutputStream()
        if (filePos.endsWith(".js", true)){
            content.write((jsHeader + headerEndTag) .toByteArray())
            content.write(getFromAssets(filePos).toByteArray())

        }else if (filePos.endsWith(".html", true) || filePos.endsWith(".htm", true)){
            content.write((htmlHeader + headerEndTag) .toByteArray())
            content.write(getFromAssets(filePos).toByteArray())
        }else if (filePos.endsWith(".css", true)){
            content.write((cssHeader + headerEndTag) .toByteArray())
            content.write(getFromAssets(filePos).toByteArray())
        }else if (filePos.endsWith(".png", true)){
            val binContent = getFromAssets(filePos).toByteArray()
            val header = pngHeader + binContent.size + "\r\n" + headerEndTag
            content.write(header.toByteArray())
            content.write(binContent)
        }
        else {
            content.write( (errorContent + "Not Found.  Tesla server!!").toByteArray())
        }
        try {
            val os = sock.getOutputStream()
            os.write(
                content.toByteArray()
            )
            os.flush()
            sock.close()
        } catch (e: Exception) {
            Log.e("SM", "Home page handler error", e)
        }
    }

    private fun getFromAssets(fileName: String): ByteArrayOutputStream {

        var out = ByteArrayOutputStream()
        try {
            val inputStream: InputStream = this.context.assets.open(fileName)
            out = toByteArray(inputStream)
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return out
    }

    @Throws(IOException::class)
    private fun toByteArray(`in`: InputStream): ByteArrayOutputStream {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024 * 4)
        var n = 0
        while (`in`.read(buffer).also { n = it } != -1) {
            out.write(buffer, 0, n)
        }
        return out
    }
}