package com.copyland.howtoh.http

import android.content.Context
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


abstract class HttpHandler(sock: Socket, context: Context) : Thread() {
    val sock: Socket
    val context:Context

    init {
        this.sock = sock
        this.context = context
    }

    companion object {
        const val SERVER_NAME = "TESLA MPEG server by Evan"
        val SERVER_TIME: String
            get() {
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat(
                    "EEE, dd MMM yyyy HH:mm:ss z", Locale.US
                )
                dateFormat.timeZone = TimeZone.getTimeZone("GMT")
                return dateFormat.format(calendar.time)
            }
    }
}
