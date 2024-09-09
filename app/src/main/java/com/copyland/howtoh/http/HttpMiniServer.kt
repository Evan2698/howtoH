package com.copyland.howtoh.http

import android.util.Log
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.Collections
import kotlin.concurrent.thread

class HttpMiniServer(port: Int, imageCache: JPEGCache) {
    private val serverPort: Int
    private val imageCache: JPEGCache
    private val connections = Collections.synchronizedSet<WSConnection?>(LinkedHashSet())


    @Volatile
    private var interrupted: Boolean = false

    companion object {
        private val TAG: String = HttpMiniServer::class.java.simpleName
        private const val DELAY_TIME:Long = 40
    }

    init {
        this.serverPort = port
        this.imageCache = imageCache
    }

    private val httpServer by lazy {
        embeddedServer(Netty, this.serverPort) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(10)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                staticResources("/", "assets/webroot")
                webSocket("/tesla") {
                    Log.d(TAG, "websocket is open!!!")
                    val thisConnection = WSConnection(this)
                    connections += thisConnection
                    try {
                        for (message in incoming) {
                            if (interrupted) {
                                break
                            }
                            message as? Frame.Text ?: continue
                            MessagePump.getInstance().dispatchMessage(message.readText())
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "websocket error", e)
                        //e.printStackTrace()
                    } finally {
                        Log.w(TAG,"Removing socket and clear the environment variables")
                        connections -= thisConnection
                    }
                }
            }
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun start() {
        this.interrupted = false
        this.imageCache.clear()
        Thread{
            while (!this.interrupted) {
                val data = this.imageCache.takeImageFromStream()
                if (data.size() == 0) {
                    Log.d("SM", "image is specified exiting flag for exit!!!!")
                    break
                }
                for (item: WSConnection in connections){
                    try {
                        notifiedEveryWebsocket(item, data)
                    }
                    catch ( e: Exception){
                        Log.d("SM", "Global notified failed:", e)
                    }
                    if (this.interrupted){
                        break
                    }
                }

                if (!this.interrupted){
                    sleepMillis()
                }
            }
            for (item: WSConnection in connections){
                GlobalScope.launch(CoroutineExceptionHandler { _, throwable ->
                    Log.d(TAG, throwable.message.toString())
                }) {
                    item.session.close()
                }
            }

            connections.clear();
            Log.d("SM", "<------------------------------------------>${this.interrupted}")
            httpServer.stop(2000, 5000)
            Thread.sleep(1000)
            Log.d("SM", "<-----------------HTTP STOP--------------->")
        }.start()


        CoroutineScope(Dispatchers.IO).launch(CoroutineExceptionHandler { _, throwable ->
            Log.d(TAG, throwable.message.toString())
        }) {
            launch {
                httpServer.start(wait = true)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun notifiedEveryWebsocket(
        item: WSConnection,
        data: ByteArrayOutputStream
    ) {
        GlobalScope.launch(CoroutineExceptionHandler { _, throwable ->
            Log.d(TAG, throwable.message.toString())
        }) {
            try {
                item.session.send(Frame.Binary(true, data.toByteArray()))
            } catch (e: Exception) {
                Log.d(TAG, "websocket send internal: ", e)
            }
        }
    }

    private fun sleepMillis() {
        Thread.sleep(DELAY_TIME)
    }

    fun stop() {
        this.interrupted = true
        imageCache.stopCapture()
    }
}