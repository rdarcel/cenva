package org.daas.tcp

import io.vertx.core.Vertx
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger;


/**
 * Receive TCP request for SIP handling
 */
@ApplicationScoped
class SipTCPReceiver {

    companion object{
        private val LOG:Logger = Logger.getLogger(SipTCPReceiver::class.java)
    }

    private val vertx: Vertx = Vertx.vertx()
    private lateinit var server: NetServer


    @Startup
    fun init() {
        LOG.debug("Starting SIP TCP receiver")
        server = vertx.createNetServer()
        server.connectHandler { socket: NetSocket ->
            handleClient(socket)
        }
        server.listen(5060) { res ->
            if (res.succeeded()) {
                LOG.info("Server is now listening on port 5060")
            } else {
                LOG.info("Failed to bind!")
            }
        }
    }

    private fun handleClient(socket: NetSocket) {
        socket.handler { buffer ->
            val message = buffer.toString()
            LOG.info("Received message: $message")
            // Process the message here
        }
        socket.closeHandler {
            LOG.info("Client disconnected")
        }
    }
}