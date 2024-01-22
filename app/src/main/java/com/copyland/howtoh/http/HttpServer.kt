package com.copyland.howtoh.http

import android.content.Context
import android.util.Log
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket


abstract class HttpServer(port: Int, context: Context) : Thread() {
    val mServerSock: ServerSocket
    private val mReadBuffer: ByteArray
    val context:Context

    init {
        mServerSock = ServerSocket(port)
        mReadBuffer = ByteArray(4096)
        this.context = context
    }

    override fun run() {
        Log.i("SM", "Server thread started")
        while (!isInterrupted) {
            try {
                // read request
                val incoming = mServerSock.accept()
                incoming.sendBufferSize = 32 * 1024
                val `is` = incoming.getInputStream()
                if (`is`.read(mReadBuffer) != -1) {
                    // parse
                    val req = mReadBuffer.toString(Charsets.UTF_8)
                    val lines = req.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    if (lines.isNotEmpty()) {
                        val head = lines[0].split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        if (head.size == 3 && head[0] == "GET") {
                            // handle GET
                            handlePath(incoming, head[1])
                        } else {
                            Log.i("SM", "Method unsupported or wrong head")
                        }
                    } else {
                        Log.i("SM", "Empty request")
                        incoming.close()
                    }
                } else {
                    Log.i("SM", "Server read error")
                }
            } catch (e: Exception) {
                Log.e("SM", "Server error", e)
                try {
                    sleep(1000)
                } catch (e1: InterruptedException) {
                    return
                }
            }
        }
        try {
            mServerSock.close()
        } catch (e: IOException) {
            Log.e("SM", "Error closing server socket", e)
        }
        Log.i("SM", "Server thread finished")
    }

    abstract fun handlePath(sock: Socket, path: String)
}
