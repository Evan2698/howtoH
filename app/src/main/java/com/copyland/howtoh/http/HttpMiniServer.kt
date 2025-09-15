package com.copyland.howtoh.http

import android.content.Context
import android.util.Log
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
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
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

class HttpMiniServer(port: Int, imageCache: JPEGCache, context: Context) {
    private val serverPort: Int
    private val imageCache: JPEGCache
    private val connections = Collections.synchronizedSet<WSConnection?>(LinkedHashSet())
    private val context:Context


    @Volatile
    private var interrupted: Boolean = false

    companion object {
        private val TAG: String = HttpMiniServer::class.java.simpleName
        private const val DELAY_TIME:Long = 40
    }

    init {
        this.serverPort = port
        this.imageCache = imageCache
        this.context = context
    }



    private fun ApplicationEngine.Configuration.envConfig() {
//        val keystoreFile: InputStream = context.resources.openRawResource(R.raw.keystore)
//
//        val keyAlias = "1"
//
//        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
//        keyStore.load(keyStoreStream, keyStorePassword)
//
//        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
//        keyManagerFactory.init(keyStore, privateKeyPassword)
//
//
//        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
//        trustManagerFactory.init(null as KeyStore?)

//        val keyStoreFile = File("build/keystore.jks")
//        val keyStore = buildKeyStore {
//            certificate("sampleAlias") {
//                password = "foobar"
//                domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
//            }
//        }
//        try {
//            keyStore.saveToFile(keyStoreFile, "123456")
//        }
//        catch ( e: Exception){
//            Log.d(TAG, "File save exception", e)
//        }



        connector {
            port = serverPort
        }

//        sslConnector(
//            keyStore = keyStore,
//            keyAlias = "sampleAlias",
//            keyStorePassword = { "123456".toCharArray() },
//            privateKeyPassword = { "foobar".toCharArray() }) {
//            port = 8443
//            keyStorePath = keyStoreFile
//
//        }
    }

    fun  buildserver():EmbeddedServer<NettyApplicationEngine, io. ktor. server. netty. NettyApplicationEngine. Configuration>{

        val httpServer = embeddedServer(Netty, applicationEnvironment{
            log = LoggerFactory.getLogger("tesla.application")
        }, configure = {
            envConfig()
        }){
            install(WebSockets) {
                pingPeriod = 15.seconds
                timeout = 15.seconds
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

        return httpServer
    }




    @OptIn(DelicateCoroutinesApi::class)
    fun start() {
        this.interrupted = false
        this.imageCache.clear()

        val httpServer = buildserver()

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