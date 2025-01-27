package org.daas

import io.vertx.core.Vertx
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger;



@ApplicationScoped
class SipTCPReceiver {

    private val vertx: Vertx = Vertx.vertx()
    private lateinit var server: NetServer

    @PostConstruct
    fun init() {
        server = vertx.createNetServer()
        server.connectHandler { socket: NetSocket ->
            handleClient(socket)
        }
        server.listen(5060) { res ->
            if (res.succeeded()) {
                println("Server is now listening on port 5060")
            } else {
                println("Failed to bind!")
            }
        }
    }

    private fun handleClient(socket: NetSocket) {
        socket.handler { buffer ->
            val message = buffer.toString()
            println("Received message: $message")
            // Process the message here
        }
        socket.closeHandler {
            println("Client disconnected")
        }
    }
}