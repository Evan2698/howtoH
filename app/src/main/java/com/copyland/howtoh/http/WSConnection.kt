package com.copyland.howtoh.http

import io.ktor.websocket.DefaultWebSocketSession

class WSConnection(private val s : DefaultWebSocketSession) {
    public var session: DefaultWebSocketSession = s
}