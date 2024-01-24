package com.copyland.howtoh.http

import android.util.Log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.Blocking
import java.lang.Thread.sleep
import java.time.Duration
import java.util.Collections
import kotlin.concurrent.thread

class HttpMiniServer constructor(port: Int, imageCache: JPEGCache) {
    private val serverPort: Int
    private val imageCache: JPEGCache
    private val connections = Collections.synchronizedSet<WSConnection?>(LinkedHashSet())
    private val waitObject = Object()
    private var launchRoutine:Thread? = null

    @Volatile
    private var interrupted: Boolean = false

    companion object {
        private val TAG: String = HttpMiniServer::class.java.simpleName
    }

    init {
        this.serverPort = port
        this.imageCache = imageCache
    }

    private val httpServer by lazy {
        embeddedServer(Netty, this.serverPort) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
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
                        for (frame in incoming) {
                            frame as? Frame.Text ?: continue
                            Log.d(TAG, frame.readText())
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "websocket error", e)
                        e.printStackTrace()
                    } finally {
                        println("Removing $thisConnection!")
                        connections -= thisConnection
                    }
                }
            }
        }
    }


    public fun start() {
        this.interrupted = false
        httpServer.start(wait = false)

        launchRoutine = Thread(Runnable {
            val obj = Object()
            while (!this.interrupted){
                val data = this.imageCache.takeImageFromStream()
                if (data.size() == 0){
                    break;
                }
                connections.forEach {
                    try{
                        runBlocking {
                            it.session.send(Frame.Binary(true, data.toByteArray()))
                        }
                    }catch (e: Exception){
                        Log.d(TAG, "websocket send failed: ", e)
                    }
                }
                sleepMillis(40)
            }
        })

        launchRoutine?.start()
    }

    private fun sleepMillis(timeout:Long) {
        try {
            waitObject.wait(timeout)
        } catch (e: Exception) {
            Log.d(TAG, "e", e)
        }
    }

    public fun stop() {
        this.interrupted = true
        Thread(Runnable{
            waitObject.notifyAll()
            sleep(500)
            waitObject.notifyAll()
            launchRoutine?.interrupt()
            launchRoutine?.join()
            httpServer.stop()
        })
    }
}