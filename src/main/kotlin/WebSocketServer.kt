import Utils.getIpAddress
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

class WebSocketServer() {
    private val connections = ConcurrentHashMap<String, WebSocketSession>()

    private val webSocketServer = embeddedServer(Netty, host = getIpAddress(), port = 8081) {
        install(WebSockets)

        routing {
            webSocket("/game") {
                val id = call.parameters["id"] ?: error("Missing id parameter")
                connections[id] = this
                try {
                    send("Connected to server!")
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            println("Received message from $id: $text")
                        }
                    }
                } finally {
                    connections.remove(id)
                }
            }
        }
    }

    suspend fun broadcast(message: String) {
        for (session in connections.values) {
            session.send(message)
        }
    }

    fun start(wait: Boolean) {
        webSocketServer.start(wait)
    }
}

